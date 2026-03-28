package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.HashMap;

import javax.sql.DataSource;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
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
}
