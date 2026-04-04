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
     * so each airport is represented by a single shared instance.
     * Returns a HashMap keyed by flight_number.
     */
    public HashMap<String, Flight> readFlights() {
        HashMap<String, Flight> flightList = new HashMap<>();
        // DISTINCT ON deduplicates by flight_number in the DB, keeping the earliest
        // date's row. This avoids streaming all 3M rows to deduplicate in Java.
        String sql = "SELECT DISTINCT ON (flight_number) flight_number, departure_time, ticket_price, origin, destination "
                + "FROM flights "
                + "WHERE stops = 0 "
                + "ORDER BY flight_number, flight_date";

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
                flightList.put(flightNumber, flight);
            }

            System.out.println("Loaded " + flightList.size() + " flights from database");

        } catch (Exception e) {
            System.out.println("Error reading flights from database: " + e.getMessage());
        }

        return flightList;
    }

    /*
     * Fetches flights from the database for a specific list of (origin, destination, date) tuples,
     * joining airlines and planes to include airline name and aircraft name.
     * Returns a HashMap keyed by "ORIGINDEST-DATE" (e.g. "YYZJFK2026-04-15").
     */
    public HashMap<String, ArrayList<Flight>> readFlightsForLegs(List<LegQuery> legs) {
        HashMap<String, ArrayList<Flight>> result = new HashMap<>();
        if (legs.isEmpty()) return result;

        StringBuilder sql = new StringBuilder(
                "SELECT f.flight_number, f.origin, f.destination, f.departure_time, " +
                "f.ticket_price, f.flight_date, a.airline_name, p.name AS aircraft_name " +
                "FROM flights f " +
                "LEFT JOIN airlines a ON f.airline_code = a.airline_code " +
                "LEFT JOIN planes p ON f.aircraft_type = p.iata_code " +
                "WHERE f.stops = 0 AND (f.origin, f.destination, f.flight_date) IN (");

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
                    String flightNumber = rs.getString("flight_number");
                    Airport origin = airportStore.getAirportByCode(rs.getString("origin"));
                    Airport destination = airportStore.getAirportByCode(rs.getString("destination"));

                    if (origin == null || destination == null) continue;

                    double distance = FlightDistanceCalculator.calcDistance(origin, destination);
                    LocalTime departureTime = rs.getTime("departure_time").toLocalTime();
                    int price = rs.getBigDecimal("ticket_price").intValue();

                    Flight flight = new Flight(origin, destination, distance, departureTime, flightNumber);
                    flight.setPrice(price);
                    flight.setAirlineName(rs.getString("airline_name"));
                    flight.setAircraftName(rs.getString("aircraft_name"));

                    String key = origin.getCode() + destination.getCode() + rs.getDate("flight_date").toLocalDate().toString();
                    result.computeIfAbsent(key, k -> new ArrayList<>()).add(flight);
                }
            }

        } catch (Exception e) {
            System.out.println("Error reading flights for legs from database: " + e.getMessage());
        }

        return result;
    }
}
