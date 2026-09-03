package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.FlightResult;

/** Handles all route-related database queries. Methods here back both the API endpoints and the terminal runner. */
public class RouteStore {

    // TODO: wire up in Server.java as: app.get("/api/routes/search", Server::searchRoutes);
    // Query param: origin (IATA code)
    // Returns: JSON array of FlightResult

    /** Returns the top 20 cheapest flights departing from the given airport, one per destination. */
    public static ArrayList<FlightResult> findFlights(String origin, Connection conn) {
        ArrayList<FlightResult> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM (" +
                "    SELECT DISTINCT ON (destination) destination, ticket_price, flight_number, airline_code, departure_time" +
                "    FROM flights" +
                "    WHERE origin = ?" +
                "    ORDER BY destination, ticket_price ASC" +
                ") cheapest" +
                " ORDER BY ticket_price ASC" +
                " LIMIT 20")) {
            ps.setString(1, origin);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new FlightResult(
                    rs.getString("flight_number"),
                    origin,
                    rs.getString("destination"),
                    rs.getInt("ticket_price"),
                    rs.getString("airline_code"),
                    rs.getTime("departure_time").toLocalTime()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
        return results;
    }

    public static ArrayList<FlightResult> findFlightHome(String currentLocation, String homeAirport, Connection conn) {
        ArrayList<FlightResult> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM (" +
                "    SELECT destination, ticket_price, flight_number, airline_code, departure_time" +
                "    FROM flights" +
                "    WHERE origin = ?" +
                "    AND destination = ?" +
                "    ORDER BY ticket_price ASC" +
                ") cheapest" +
                " ORDER BY ticket_price ASC" +
                " LIMIT 20")) {
            ps.setString(1, currentLocation);
            ps.setString(2, homeAirport);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new FlightResult(
                    rs.getString("flight_number"),
                    currentLocation,
                    rs.getString("destination"),
                    rs.getInt("ticket_price"),
                    rs.getString("airline_code"),
                    rs.getTime("departure_time").toLocalTime()
                ));
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
        return results;
    }


    /** Returns true if the given IATA code exists in the airports table. */
    public static boolean validateAirport(String airport, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT iata_code FROM airports WHERE iata_code = ?")) {
        ps.setString(1, airport);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            //System.out.println(rs.getString("iata_code") + " is a valid airport...");
            return true;
        } else {
            return false;
            //System.err.println(homeAirport + " was not found...");
        }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        } 
        return false;
    }

    /** Returns a fully populated Airport object for the given IATA code, or null if not found. */
    public static Airport getAirport(String iataCode, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT iata_code, name, latitude, longitude, max_runway_length_ft, elevation_ft, city, country" +
                " FROM airports WHERE iata_code = ?")) {
            ps.setString(1, iataCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Airport(
                    rs.getString("iata_code"),
                    rs.getString("name"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getInt("max_runway_length_ft"),
                    rs.getInt("elevation_ft"),
                    rs.getString("city"),
                    rs.getString("country")
                );
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
        return null;
    }

    /** Converts a FlightResult into a Flight object, calculating distance via Haversine. */
    public static Flight convertToFlight(FlightResult fr, Connection conn) {
        Airport origin = getAirport(fr.origin(), conn);
        Airport destination = getAirport(fr.destination(), conn);
        if (origin == null || destination == null) {
            System.out.println("Could not resolve airport for flight " + fr.flightNumber());
            return null;
        }
        double distance = FlightDistanceCalculator.calcDistance(origin, destination);
        Flight f = new Flight(origin, destination, distance, fr.departureTime(), fr.flightNumber());
        f.setPrice(fr.price());
        f.setAirlineName(fr.airline());
        return f;
    }
}
