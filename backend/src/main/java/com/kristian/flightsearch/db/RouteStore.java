package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kristian.flightsearch.datagenerator.FlightDistanceCalculator;
import com.kristian.flightsearch.datagenerator.FlightDurationCalculator;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.FlightResult;
import com.kristian.flightsearch.models.FlyHomeResult;
import com.kristian.flightsearch.models.RouteSearchResult;

/** Handles all route-related database queries. Methods here back both the API endpoints and the terminal runner. */
public class RouteStore {

    /** Returns the top 20 cheapest flights departing from the given airport, one per destination.
     *  Used by the terminal RouteBuilder. */
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

    /** Returns the top 20 cheapest flights out of origin, one per destination, enriched with city name,
     *  distance, and duration. Destinations in the exclude list are omitted. Used by the route-builder API. */
    public static List<RouteSearchResult> searchFlights(String origin, List<String> exclude, Connection conn) {
        Airport originAirport = getAirport(origin, conn);
        if (originAirport == null) return List.of();

        String excludeClause = exclude.isEmpty()
            ? ""
            : " AND f.destination NOT IN (" + String.join(", ", Collections.nCopies(exclude.size(), "?")) + ")";

        String sql =
            "SELECT * FROM (" +
            "    SELECT DISTINCT ON (f.destination)" +
            "        f.flight_number, f.airline_code, f.destination, f.ticket_price, f.departure_time," +
            "        a.city AS destination_city," +
            "        a.latitude AS dest_lat," +
            "        a.longitude AS dest_lon" +
            "    FROM flights f" +
            "    JOIN airports a ON f.destination = a.iata_code" +
            "    WHERE f.origin = ?" +
            excludeClause +
            "    ORDER BY f.destination, f.ticket_price ASC" +
            ") cheapest" +
            " ORDER BY ticket_price ASC" +
            " LIMIT 20";

        List<RouteSearchResult> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            ps.setString(paramIndex++, origin);
            for (String code : exclude) {
                ps.setString(paramIndex++, code);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String destination = rs.getString("destination");
                String destinationCity = rs.getString("destination_city");
                double destLat = rs.getDouble("dest_lat");
                double destLon = rs.getDouble("dest_lon");

                Airport destAirport =
                    new Airport(destination, destinationCity, destLat, destLon, 0, 0, destinationCity, "");
                int distanceKm = (int) FlightDistanceCalculator.calcDistance(originAirport, destAirport);
                int durationMinutes = (int) FlightDurationCalculator.calculateFlightDuration(distanceKm).toMinutes();

                results.add(new RouteSearchResult(
                    rs.getString("flight_number"),
                    origin,
                    destination,
                    destinationCity,
                    rs.getInt("ticket_price"),
                    rs.getString("airline_code"),
                    rs.getTime("departure_time").toLocalTime(),
                    distanceKm,
                    durationMinutes
                ));
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
        return results;
    }

    /** Returns cheapest direct flight from currentLocation to homeAirport, or empty if none exists.
     *  Falls back to Dijkstra on the flight graph when no direct flight is found. */
    public static FlyHomeResult routeHome(String from, String home, FlightGraph graph, Connection conn) {
        Optional<RouteSearchResult> direct = findDirectHome(from, home, conn);
        if (direct.isPresent()) {
            RouteSearchResult leg = direct.get();
            return new FlyHomeResult(true, List.of(leg), leg.price());
        }

        AirportVertex fromVertex = graph.getVertex(from);
        AirportVertex homeVertex = graph.getVertex(home);
        if (fromVertex == null || homeVertex == null) {
            return new FlyHomeResult(false, List.of(), 0);
        }

        Map[] dijkstraResult = Dijkstra.searchByPrice(graph, fromVertex);
        @SuppressWarnings("unchecked")
        Map<Airport, Integer> prices =
            (Map<Airport, Integer>) dijkstraResult[0];
        @SuppressWarnings("unchecked")
        Map<Airport, AirportVertex> previous =
            (Map<Airport, AirportVertex>) dijkstraResult[1];

        Airport homeAirport = homeVertex.getData();
        Integer totalCost = prices.get(homeAirport);
        if (totalCost == null || totalCost == Integer.MAX_VALUE) {
            return new FlyHomeResult(false, List.of(), 0);
        }

        List<Airport> path = reconstructPath(previous, homeAirport, fromVertex.getData());
        if (path.size() < 2) {
            return new FlyHomeResult(false, List.of(), 0);
        }

        List<RouteSearchResult> legs = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Optional<RouteSearchResult> leg = findDirectHome(path.get(i).getCode(), path.get(i + 1).getCode(), conn);
            if (leg.isEmpty()) return new FlyHomeResult(false, List.of(), 0);
            legs.add(leg.get());
            total += leg.get().price();
        }
        return new FlyHomeResult(false, legs, total);
    }

    /** Cheapest single direct flight from origin to destination, or empty if none exists. */
    private static Optional<RouteSearchResult> findDirectHome(String from, String to, Connection conn) {
        Airport originAirport = getAirport(from, conn);
        Airport destAirport = getAirport(to, conn);
        if (originAirport == null || destAirport == null) return Optional.empty();

        int distanceKm = (int) FlightDistanceCalculator.calcDistance(originAirport, destAirport);
        int durationMinutes = (int) FlightDurationCalculator.calculateFlightDuration(distanceKm).toMinutes();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT f.flight_number, f.airline_code, f.ticket_price, f.departure_time," +
                "       a.city AS destination_city" +
                " FROM flights f" +
                " JOIN airports a ON f.destination = a.iata_code" +
                " WHERE f.origin = ? AND f.destination = ?" +
                " ORDER BY f.ticket_price ASC" +
                " LIMIT 1")) {
            ps.setString(1, from);
            ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new RouteSearchResult(
                    rs.getString("flight_number"),
                    from,
                    to,
                    rs.getString("destination_city"),
                    rs.getInt("ticket_price"),
                    rs.getString("airline_code"),
                    rs.getTime("departure_time").toLocalTime(),
                    distanceKm,
                    durationMinutes
                ));
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Walks the Dijkstra previous-vertex map backwards from destination to source, returning the ordered path. */
    private static List<Airport> reconstructPath(
            Map<Airport, AirportVertex> previous,
            Airport destination,
            Airport source) {
        List<Airport> path = new ArrayList<>();
        Airport current = destination;
        while (current != null && !current.equals(source)) {
            path.add(0, current);
            AirportVertex prev = previous.get(current);
            current = (prev != null) ? prev.getData() : null;
        }
        if (current != null) path.add(0, source);
        return path;
    }

    /** Returns all direct flights from currentLocation to homeAirport. Used by the terminal RouteBuilder. */
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
            return rs.next();
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

    /** Converts a FlightResult into a Flight object, calculating distance using the Haversine formula. */
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
