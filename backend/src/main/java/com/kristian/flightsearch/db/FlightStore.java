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

/*
 * Handles reading and writing flight data to the database.
 */

public class FlightStore {

    private final DataSource dataSource;
    private final AirportStore airportStore;

    public FlightStore(DataSource dataSource, AirportStore airportStore) {
        this.dataSource = dataSource;
        this.airportStore = airportStore;
    }

    /*
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
    public HashMap<String, Flight> readFlights() {
        HashMap<String, Flight> flightList = new HashMap<>();
        String sql = "SELECT f.flight_number, f.departure_time, f.ticket_price, "
                + "f.origin, f.destination, a.airline_name, p.name AS aircraft_name "
                + "FROM flights f "
                + "LEFT JOIN airlines a ON f.airline_code = a.airline_code "
                + "LEFT JOIN planes p ON f.aircraft_type = p.iata_code";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String flightNumber = rs.getString("flight_number");
                Airport origin = airportStore.getAirportByCode(rs.getString("origin"));
                Airport destination = airportStore.getAirportByCode(rs.getString("destination"));

                if (origin == null || destination == null) continue;

                double distance = FlightDistanceCalculator.calcDistance(origin, destination);
                LocalTime departureTime = rs.getTime("departure_time").toLocalTime();
                int price = rs.getBigDecimal("ticket_price").intValue();

                // Flight constructor calculates duration and arrival time from distance
                Flight flight = new Flight(origin, destination, distance, departureTime, flightNumber);
                flight.setPrice(price);
                flight.setAirlineName(rs.getString("airline_name"));
                flight.setAircraftName(rs.getString("aircraft_name"));
                flightList.put(flightNumber, flight);
            }

            System.out.println("Loaded " + flightList.size() + " flights from database");

        } catch (Exception e) {
            System.out.println("Error reading flights from database: " + e.getMessage());
        }

        return flightList;
    }

}
