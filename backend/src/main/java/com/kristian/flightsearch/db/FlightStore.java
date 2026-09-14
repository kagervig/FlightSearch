package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
import com.kristian.flightsearch.datagenerator.FlightDurationCalculator;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.LegQuery;

/*
 * Handles reading flight data from the database.
 */

public class FlightStore {

    private final DataSource dataSource;
    private final AirportStore airportStore;

    public FlightStore(DataSource dataSource, AirportStore airportStore) {
        this.dataSource = dataSource;
        this.airportStore = airportStore;
    }

    /*
     * Returns all distinct (origin, destination) pairs for direct flights.
     * Used at startup to build the connectivity graph without loading flight objects.
     * Queries all departures (origin = airportCode) and arrivals (destination = airportCode),
     * computes arrival times, and returns them as two sorted lists.
     * Rows where either airport is not found in AirportStore are skipped.
     * Returns empty lists on SQL error.
     */
    public Map<String, List<Map<String, Object>>> readFlightsForBoard(String airportCode) {
        List<Map<String, Object>> departures = new ArrayList<>();
        List<Map<String, Object>> arrivals = new ArrayList<>();

        String departuresSql = "SELECT f.flight_number, f.departure_time, f.ticket_price, "
                + "f.origin, f.destination, a.airline_name, p.name AS aircraft_name "
                + "FROM flights f "
                + "LEFT JOIN airlines a ON f.airline_code = a.airline_code "
                + "LEFT JOIN planes p ON f.aircraft_type = p.iata_code "
                + "WHERE f.origin = ? "
                + "ORDER BY f.departure_time";

        String arrivalsSql = "SELECT f.flight_number, f.departure_time, f.ticket_price, "
                + "f.origin, f.destination, a.airline_name, p.name AS aircraft_name "
                + "FROM flights f "
                + "LEFT JOIN airlines a ON f.airline_code = a.airline_code "
                + "LEFT JOIN planes p ON f.aircraft_type = p.iata_code "
                + "WHERE f.destination = ? "
                + "ORDER BY f.departure_time";

        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(departuresSql)) {
                pstmt.setString(1, airportCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = buildBoardRow(rs);
                        if (row != null) departures.add(row);
                    }
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(arrivalsSql)) {
                pstmt.setString(1, airportCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = buildBoardRow(rs);
                        if (row != null) arrivals.add(row);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading flights for board: " + e.getMessage());
            return Map.of("departures", List.of(), "arrivals", List.of());
        }

        return Map.of("departures", departures, "arrivals", arrivals);
    }

    /**
     * takes as input the results from the sql query, row by row
     * fetches airport objects of the origin and destination airports
     * calculates flight distance using the haversine formula in calcDistance method 
     * calculates flight duration based on distance using calculateFlightDuration
     * builds a hashmap of all data to be displayed in that row
     * returns the hashmap
     * @param rs
     * @return
     * @throws Exception
     */

    private Map<String, Object> buildBoardRow(ResultSet rs) throws Exception {
        Airport origin = airportStore.getAirportByCode(rs.getString("origin"));
        Airport destination = airportStore.getAirportByCode(rs.getString("destination"));

        if (origin == null || destination == null) return null;

        double distance = FlightDistanceCalculator.calcDistance(origin, destination);
        Duration duration = FlightDurationCalculator.calculateFlightDuration(distance);
        LocalTime departure = rs.getTime("departure_time").toLocalTime();
        LocalTime arrival = departure.plus(duration);

        Map<String, Object> row = new HashMap<>();
        row.put("flightNumber", rs.getString("flight_number"));
        row.put("airlineName", rs.getString("airline_name"));
        row.put("aircraftName", rs.getString("aircraft_name"));
        row.put("origin", origin.getCode());
        row.put("originCity", origin.getCity());
        row.put("destination", destination.getCode());
        row.put("destinationCity", destination.getCity());
        row.put("departureTime", String.format("%02d:%02d", departure.getHour(), departure.getMinute()));
        row.put("arrivalTime", String.format("%02d:%02d", arrival.getHour(), arrival.getMinute()));
        row.put("durationMinutes", duration.toMinutes());
        row.put("price", rs.getBigDecimal("ticket_price").intValue());
        return row;
    }

    /*
     * Reads all flights from the database. Airport objects are resolved from AirportStore
     * so each airport is represented by a single shared instance. Joins airlines and
     * planes so airlineName and aircraftName are available for display without a
     * second query.
     * Returns a HashMap keyed by flight_number.
     */
    public List<String[]> getConnectionMap() {
        List<String[]> connections = new ArrayList<>();
        String sql = "SELECT DISTINCT origin, destination FROM flights";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                connections.add(new String[]{ rs.getString("origin"), rs.getString("destination") });
            }

        } catch (Exception e) {
            System.out.println("Error loading connection map: " + e.getMessage());
        }

        return connections;
    }

    /*
     * Fetches full Flight objects for a list of (origin, destination) legs.
     * Returns a map keyed by "ORIGINDEST" (e.g. "YYZJFK")
     * whose values are lists of Flight objects with price and metadata populated.
     */
    public HashMap<String, ArrayList<Flight>> readFlightsForLegs(List<LegQuery> legs) {
        HashMap<String, ArrayList<Flight>> result = new HashMap<>();
        if (legs.isEmpty()) return result;

        StringBuilder sql = new StringBuilder(
                "SELECT f.flight_number, f.departure_time, f.ticket_price, " +
                "f.origin, f.destination, a.airline_name, p.name AS aircraft_name " +
                "FROM flights f " +
                "LEFT JOIN airlines a ON f.airline_code = a.airline_code " +
                "LEFT JOIN planes p ON f.aircraft_type = p.iata_code " +
                "WHERE (f.origin, f.destination) IN (");

        for (int i = 0; i < legs.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?)");
        }
        sql.append(")");

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            for (LegQuery leg : legs) {
                pstmt.setString(paramIdx++, leg.origin());
                pstmt.setString(paramIdx++, leg.destination());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Airport origin = airportStore.getAirportByCode(rs.getString("origin"));
                    Airport destination = airportStore.getAirportByCode(rs.getString("destination"));
                    if (origin == null || destination == null) continue;

                    double distance = FlightDistanceCalculator.calcDistance(origin, destination);
                    LocalTime departureTime = rs.getTime("departure_time").toLocalTime();
                    int price = rs.getBigDecimal("ticket_price").intValue();

                    Flight flight = new Flight(origin, destination, distance, departureTime, rs.getString("flight_number"));
                    flight.setPrice(price);
                    flight.setAirlineName(rs.getString("airline_name"));
                    flight.setAircraftName(rs.getString("aircraft_name"));

                    String key = rs.getString("origin") + rs.getString("destination");
                    result.computeIfAbsent(key, k -> new ArrayList<>()).add(flight);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading flights for legs from database: " + e.getMessage());
        }

        return result;
    }

    /*
     * Fetches all direct flights between two airports.
     * Used by the /api/flights/search endpoint.
     */
    public ArrayList<Flight> getFlightsForRoute(String origin, String destination) {
        ArrayList<Flight> flights = new ArrayList<>();

        Airport originAirport = airportStore.getAirportByCode(origin);
        Airport destinationAirport = airportStore.getAirportByCode(destination);
        if (originAirport == null || destinationAirport == null) return flights;

        double distance = FlightDistanceCalculator.calcDistance(originAirport, destinationAirport);

        String sql = "SELECT f.flight_number, f.departure_time, f.ticket_price, " +
                     "a.airline_name, p.name AS aircraft_name " +
                     "FROM flights f " +
                     "LEFT JOIN airlines a ON f.airline_code = a.airline_code " +
                     "LEFT JOIN planes p ON f.aircraft_type = p.iata_code " +
                     "WHERE f.origin = ? AND f.destination = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, origin);
            pstmt.setString(2, destination);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalTime departureTime = rs.getTime("departure_time").toLocalTime();
                    int price = rs.getBigDecimal("ticket_price").intValue();

                    Flight flight = new Flight(originAirport, destinationAirport, distance,
                            departureTime, rs.getString("flight_number"));
                    flight.setPrice(price);
                    flight.setAirlineName(rs.getString("airline_name"));
                    flight.setAircraftName(rs.getString("aircraft_name"));
                    flights.add(flight);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading flights for route from database: " + e.getMessage());
        }

        return flights;
    }
}
