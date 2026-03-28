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

    public FlightStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * Reads flights from the new schema, joining airports to compute distance.
     * Deduplicates by flight_number — keeps the first row encountered (ordered by
     * flight_number, flight_date). Returns a HashMap keyed by flight_number.
     */
    public HashMap<String, Flight> readFlights() {
        HashMap<String, Flight> flightList = new HashMap<>();
        String sql = "SELECT f.flight_number, f.departure_time, f.ticket_price, "
                + "a_orig.iata_code AS origin_code, a_orig.name AS origin_name, "
                + "a_orig.city AS origin_city, a_orig.country AS origin_country, "
                + "a_orig.latitude AS origin_lat, a_orig.longitude AS origin_lon, "
                + "a_orig.timezone AS origin_tz, a_orig.elevation_ft AS origin_elev, "
                + "a_orig.max_runway_length_ft AS origin_runway, "
                + "a_dest.iata_code AS dest_code, a_dest.name AS dest_name, "
                + "a_dest.city AS dest_city, a_dest.country AS dest_country, "
                + "a_dest.latitude AS dest_lat, a_dest.longitude AS dest_lon, "
                + "a_dest.timezone AS dest_tz, a_dest.elevation_ft AS dest_elev, "
                + "a_dest.max_runway_length_ft AS dest_runway "
                + "FROM flights f "
                + "JOIN airports a_orig ON a_orig.iata_code = f.origin "
                + "JOIN airports a_dest ON a_dest.iata_code = f.destination "
                + "WHERE f.stops = 0 "
                + "ORDER BY f.flight_number, f.flight_date";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String flightNumber = rs.getString("flight_number");
                if (flightList.containsKey(flightNumber)) continue;

                Airport origin = new Airport(
                        rs.getString("origin_code"), rs.getString("origin_name"),
                        rs.getDouble("origin_lat"), rs.getDouble("origin_lon"),
                        rs.getInt("origin_runway"), rs.getInt("origin_elev"),
                        rs.getString("origin_city"), rs.getString("origin_country"));

                Airport destination = new Airport(
                        rs.getString("dest_code"), rs.getString("dest_name"),
                        rs.getDouble("dest_lat"), rs.getDouble("dest_lon"),
                        rs.getInt("dest_runway"), rs.getInt("dest_elev"),
                        rs.getString("dest_city"), rs.getString("dest_country"));

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
