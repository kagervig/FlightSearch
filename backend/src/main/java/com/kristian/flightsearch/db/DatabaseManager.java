package com.kristian.flightsearch.db;

import com.kristian.flightsearch.models.Flight;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;

public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void initialize(HashMap<String, Flight> flightList) {
        HikariConfig config = new HikariConfig();

        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null) {
            // Railway provides postgresql:// or postgres:// — convert to JDBC format
            String jdbcUrl = dbUrl
                .replace("postgresql://", "jdbc:postgresql://")
                .replace("postgres://", "jdbc:postgresql://");
            config.setJdbcUrl(jdbcUrl);
        } else {
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/flightsearch");
        }

        dataSource = new HikariDataSource(config);
        System.out.println("Database connection established");

        runMigration();

        if (isFlightsTableEmpty()) {
            seedFlights(flightList);
        } else {
            System.out.println("Flights table already populated, skipping seed");
        }
    }

    private static void runMigration() {
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("db/001_create_flights.sql")) {
            if (is == null) {
                System.out.println("Migration file not found");
                return;
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("Migration applied");
            }
        } catch (Exception e) {
            System.out.println("Migration failed: " + e.getMessage());
        }
    }

    private static boolean isFlightsTableEmpty() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM flights")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (Exception e) {
            System.out.println("Error checking flights table: " + e.getMessage());
        }
        return true;
    }

    private static void seedFlights(HashMap<String, Flight> flightList) {
        String sql = "INSERT INTO flights (scheduled_departure, scheduled_arrival, flight_number, origin, destination, price, currency, duration) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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

    public static DataSource getDataSource() {
        return dataSource;
    }
}
