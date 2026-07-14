package com.kristian.flightsearch.db;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/*
ERIN'S FUTURE FEEDBACK
1. need to make a database of airports
2. modify readflights function so that it doesn't need airport objects passed in. We can pull any other info needed from the db.
*/

public class DatabaseManager {

    private static HikariDataSource dataSource;

    private static final String[] MIGRATIONS = {
            "db/001_create_planes.sql",
            "db/002_create_airports.sql",
            "db/003_create_airlines.sql",
            "db/004_create_flights.sql"
    };

    public static void initialize() {
        // Calls all methods required to get the database running correctly
        connectToDatabase();
        runMigrations();
    }

    public static void connectToDatabase() {
        /* Establishes connection with PostgresDB */

        HikariConfig config = new HikariConfig();

        String host = System.getenv("DB_HOST");
        String port = System.getenv("DB_PORT");
        String name = System.getenv("DB_NAME");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (host != null && user != null && password != null) {
            // Individual credentials — avoids URL parsing issues with special characters
            String dbPort = (port != null) ? port : "5432";
            String dbName = (name != null) ? name : "postgres";
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + dbPort + "/" + dbName + "?sslmode=require");
            config.setUsername(user);
            config.setPassword(password);
        } else {
            String dbUrl = System.getenv("DATABASE_URL");
            if (dbUrl != null) {
                // Parse the URL ourselves so the JDBC driver receives credentials
                // separately — avoids driver mis-parsing of the userinfo component
                try {
                    URI uri = new URI(dbUrl.replace("postgresql://", "http://").replace("postgres://", "http://"));
                    String dbHost = uri.getHost();
                    int dbPort = uri.getPort() != -1 ? uri.getPort() : 5432;
                    String dbName = uri.getPath().replaceFirst("^/", "");
                    String userInfo = uri.getUserInfo();
                    int colon = userInfo.indexOf(':');
                    String dbUser = userInfo.substring(0, colon);
                    String dbPass = userInfo.substring(colon + 1);
                    config.setJdbcUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName + "?sslmode=require");
                    config.setUsername(dbUser);
                    config.setPassword(dbPass);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse DATABASE_URL: " + e.getMessage(), e);
                }
            } else {
                config.setJdbcUrl("jdbc:postgresql://localhost:5432/flightsearch");
            }
        }

        dataSource = new HikariDataSource(config);
        System.out.println("Database connection established");

    }
    
    /*
    * Runs all migrations in order. Each migration file is applied once on startup.
    */
    private static void runMigrations() {
        
        for (String migrationFile : MIGRATIONS) {
            try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream(migrationFile)) {
                if (is == null) {
                    System.out.println("Migration file not found: " + migrationFile);
                    continue;
                }
                String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                try (Connection conn = dataSource.getConnection();
                        Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    System.out.println("Applied migration: " + migrationFile);
                }
            } catch (Exception e) {
                System.out.println("Migration failed (" + migrationFile + "): " + e.getMessage());
            }
        }
    }

    /*
    * Returns true if the airports table has been seeded with data.
    */
    public static boolean areNewTablesPopulated() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM airports")) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.out.println("Error checking airports table: " + e.getMessage());
        }
        return false;
    }

    /*
    * Checks if the flights table in database is empty. Server.java calls this and
    * populates the table if necessary
    */
    public static boolean isFlightsTableEmpty() {
        
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

    public static DataSource getDataSource() {
        return dataSource;
    }
}
