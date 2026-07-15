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
