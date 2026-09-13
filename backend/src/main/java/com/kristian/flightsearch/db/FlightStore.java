package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
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
