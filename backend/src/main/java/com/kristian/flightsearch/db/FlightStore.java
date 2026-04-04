package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.LegQuery;

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
     * Reads one row per distinct flight_number from the database (direct flights only),
     * keeping the earliest date's data. Airport objects are resolved from AirportStore
     * so each airport is represented by a single shared instance. Joins airlines and
     * planes so airlineName and aircraftName are available for display without a
     * second query.
     * Returns a HashMap keyed by flight_number.
     */
    public HashMap<String, Flight> readFlights() {
        HashMap<String, Flight> flightList = new HashMap<>();
        // DISTINCT ON deduplicates by flight_number in the DB, keeping the earliest
        // date's row. This avoids streaming all 3M rows to deduplicate in Java.
        String sql = "SELECT DISTINCT ON (f.flight_number) f.flight_number, f.departure_time, f.ticket_price, "
                + "f.origin, f.destination, a.airline_name, p.name AS aircraft_name "
                + "FROM flights f "
                + "LEFT JOIN airlines a ON f.airline_code = a.airline_code "
                + "LEFT JOIN planes p ON f.aircraft_type = p.iata_code "
                + "WHERE f.stops = 0 "
                + "ORDER BY f.flight_number, f.flight_date";

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

    /*
     * Fetches date-specific prices for a list of (origin, destination, date) legs.
     * Returns a map keyed by "ORIGINDESTDATE" (e.g. "YYZJFK2026-04-15")
     * whose values are {flightNumber → price}. Callers resolve full Flight
     * objects from the in-memory index and apply these prices.
     */
    public HashMap<String, Map<String, Integer>> readFlightsForLegs(List<LegQuery> legs) {
        HashMap<String, Map<String, Integer>> result = new HashMap<>();
        if (legs.isEmpty()) return result;

        StringBuilder sql = new StringBuilder(
                "SELECT flight_number, origin, destination, flight_date, ticket_price " +
                "FROM flights " +
                "WHERE stops = 0 AND (origin, destination, flight_date) IN (");

        for (int i = 0; i < legs.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?)");
        }
        sql.append(")");

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            for (LegQuery leg : legs) {
                pstmt.setString(paramIdx++, leg.origin());
                pstmt.setString(paramIdx++, leg.destination());
                pstmt.setDate(paramIdx++, Date.valueOf(leg.date()));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("origin") + rs.getString("destination")
                            + rs.getDate("flight_date").toLocalDate().toString();
                    int price = rs.getBigDecimal("ticket_price").intValue();
                    result.computeIfAbsent(key, k -> new HashMap<>())
                            .put(rs.getString("flight_number"), price);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading flights for legs from database: " + e.getMessage());
        }

        return result;
    }
}
