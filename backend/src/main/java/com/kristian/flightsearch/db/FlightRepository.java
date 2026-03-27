package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

import javax.sql.DataSource;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class FlightRepository {

    private final DataSource dataSource;

    public FlightRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void seedFlights(HashMap<String, Flight> flightList) {
        String sql = "INSERT INTO flights (scheduled_departure, scheduled_arrival, flight_number, origin, destination, price, currency, duration, distance) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDate baseDate = LocalDate.now();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int count = 0;

            for (Flight flight : flightList.values()) {
                LocalDateTime departure = LocalDateTime.of(baseDate, flight.getDepartureTime());

                // If arrival is earlier than departure, the flight lands the next day
                LocalDate arrivalDate = flight.getArrivalTime().isBefore(flight.getDepartureTime())
                    ? baseDate.plusDays(1)
                    : baseDate;
                LocalDateTime arrival = LocalDateTime.of(arrivalDate, flight.getArrivalTime());

                pstmt.setTimestamp(1, Timestamp.valueOf(departure));
                pstmt.setTimestamp(2, Timestamp.valueOf(arrival));
                pstmt.setString(3, flight.getFlightNumber());
                pstmt.setString(4, flight.getOrigin().getCode());
                pstmt.setString(5, flight.getDestination().getCode());
                pstmt.setInt(6, flight.getPrice());
                pstmt.setString(7, "USD");
                pstmt.setInt(8, (int) flight.getDuration().toMinutes());
                pstmt.setDouble(9, flight.getDistance());
                pstmt.addBatch();

                count++;
                if (count % 500 == 0) {
                    pstmt.executeBatch();
                }
            }

            pstmt.executeBatch();
            conn.commit();
            System.out.println("Seeded " + count + " flights into database");

        } catch (Exception e) {
            System.out.println("Error seeding flights: " + e.getMessage());
        }
    }

    public HashMap<String, Flight> readFlights(Airport[] airports) {
        // Build a map for O(1) airport lookup by code
        HashMap<String, Airport> airportMap = new HashMap<>();
        for (Airport a : airports) {
            airportMap.put(a.getCode(), a);
        }

        HashMap<String, Flight> flightList = new HashMap<>();
        String sql = "SELECT flight_number, origin, destination, distance, scheduled_departure, price FROM flights";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String flightNumber = rs.getString("flight_number");
                Airport origin = airportMap.get(rs.getString("origin"));
                Airport destination = airportMap.get(rs.getString("destination"));
                double distance = rs.getDouble("distance");
                LocalTime departureTime = rs.getTimestamp("scheduled_departure").toLocalDateTime().toLocalTime();
                int price = rs.getInt("price");

                if (origin != null && destination != null) {
                    // Flight constructor calculates duration and arrival time from distance
                    Flight flight = new Flight(origin, destination, distance, departureTime, flightNumber);
                    flight.setPrice(price);
                    flightList.put(flightNumber, flight);
                }
            }

            System.out.println("Loaded " + flightList.size() + " flights from database");

        } catch (Exception e) {
            System.out.println("Error reading flights from database: " + e.getMessage());
        }

        return flightList;
    }
}
