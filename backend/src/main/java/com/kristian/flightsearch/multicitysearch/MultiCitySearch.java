package com.kristian.flightsearch.multicitysearch;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.db.FlightStore;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;

/*
 * Finds valid multi-city routes and sorts them by cheapest total price.
 * Given a home airport and a set of destinations, it generates all possible
 * orderings and filters out any where a direct flight doesn't exist for every leg.
 *
 * Steps:
 *   1) Generate all permutations of the destination airports
 *   2) Remove permutations where any leg has no available direct flight
 *   3) For each valid permutation, collect the available flights per leg
 *   4) Sort results cheapest-first
 */
public class MultiCitySearch {

    private final AirportStore airportStore;
    final HashMap<String, ArrayList<Flight>> flightIndex;

    public MultiCitySearch(AirportStore airportStore, FlightStore flightStore) {
        this.airportStore = airportStore;
        HashMap<String, Flight> flightList = flightStore.readFlights();
        this.flightIndex = FlightGenerator.flightMapper(flightList);
    }

    public MultiCitySearch(AirportStore airportStore, HashMap<String, ArrayList<Flight>> flightIndex) {
        this.airportStore = airportStore;
        this.flightIndex = flightIndex;
    }

    /**
     * Searches for all valid multi-city routes, sorted by cheapest total price.
     *
     * @param homeAirport  The origin/return airport code (e.g. "YYZ")
     * @param destinations Array of destination airport codes to visit
     * @return Sorted list of valid routes (cheapest first), empty if none found
     */
    public ArrayList<Route> search(String homeAirport, String[] destinations) {
        ArrayList<String[]> combinations = flightCombinations(destinations, homeAirport);

        // TODO: Generate the flight Index with the database
        // flightIndex = buildFlightIndexForRoute(combinations);

        // remove routes where any leg has no available flight
        for (int i = combinations.size() - 1; i >= 0; i--) {
            if (!hasFlightsForAllLegs(combinations.get(i), flightIndex)) {
                combinations.remove(i);
            }
        }

        ArrayList<Route> validRoutes = new ArrayList<>();
        for (int i = 0; i < combinations.size(); i++) {
            String[] route = combinations.get(i);
            ArrayList<ArrayList<Flight>> routeFlights = new ArrayList<>();
            for (int j = 0; j < route.length - 1; j++) {
                ArrayList<Flight> legFlights = FlightGenerator.flightRouteSearch(flightIndex, route[j], route[j + 1]);
                if (legFlights != null) {
                    routeFlights.add(legFlights);
                } else {
                    routeFlights.add(new ArrayList<>());
                }
            }
            validRoutes.add(new Route(route, routeFlights));
        }

        validRoutes.sort((a, b) -> Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        return validRoutes;
    }

    public static ArrayList<String[]> flightCombinations(String[] airportsToVisit, String homeAirport) {
        // Takes an array of destination airports to visit and returns all possible
        // combinations of routes between them
        ArrayList<String[]> flightCombinations = new ArrayList<>();

        // first and last airport always the same
        // edge case - only one airport to visit.
        if (airportsToVisit.length == 1) {
            String[] flightRoute = { homeAirport, airportsToVisit[0], homeAirport };
            flightCombinations.add(flightRoute);
            return flightCombinations;
        }

        // Generate all permutations of citiesToVisit
        permuteRoutes(airportsToVisit, 0, flightCombinations, homeAirport);
        return flightCombinations;
    }

    public HashMap<String, ArrayList<Flight>> buildFlightIndexForRoute(String[] flightRoute) {
        // builds a flightIndex from the database containing only flights between the
        // airports in flightRoute
        // flightindex enables O(1) access to flights

        HashSet<String> uniqueAirports = new HashSet<>(Arrays.asList(flightRoute));

        // Build a lookup map limited to the airports we care about
        HashMap<String, Airport> airportMap = new HashMap<>();
        for (Airport a : airportStore.getAirports()) {
            if (uniqueAirports.contains(a.getCode())) {
                airportMap.put(a.getCode(), a);
            }
        }

        HashMap<String, ArrayList<Flight>> flightIndex = new HashMap<>();
        String sql = "SELECT flight_number, origin, destination, distance, scheduled_departure, price " +
                "FROM flights WHERE origin = ANY(?) AND destination = ANY(?)";

        try (Connection conn = DatabaseManager.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Array sqlArray = conn.createArrayOf("varchar", uniqueAirports.toArray(new String[0]));
            pstmt.setArray(1, sqlArray);
            pstmt.setArray(2, sqlArray);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String flightNumber = rs.getString("flight_number");
                    Airport origin = airportMap.get(rs.getString("origin"));
                    Airport destination = airportMap.get(rs.getString("destination"));
                    double distance = rs.getDouble("distance");
                    LocalTime departureTime = rs.getTimestamp("scheduled_departure").toLocalDateTime().toLocalTime();
                    int price = rs.getInt("price");

                    if (origin != null && destination != null) {
                        Flight flight = new Flight(origin, destination, distance, departureTime, flightNumber);
                        flight.setPrice(price);

                        String key = origin.getCode() + destination.getCode();
                        flightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(flight);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error building flight index from database: " + e.getMessage());
        }

        return flightIndex;
    }

    public static boolean hasFlightsForAllLegs(String[] flightRoute, HashMap<String, ArrayList<Flight>> flightIndex) {
        // check if a flight exists that connects each airport on the route
        int numFlights = flightRoute.length;
        for (int i = 0; i < numFlights - 1; i++) {
            String key = flightRoute[i] + flightRoute[i + 1];
            if (!flightIndex.containsKey(key)) {
                return false; // If any leg is missing, route is not possible
            }
        }
        return true; // All legs exist
    }

    private static void permuteRoutes(String[] cities, int start, ArrayList<String[]> routes, String home) {
        // Helper method to generate all possible permutations of the trip to all
        // specified destinations
        // For 5 target destinations this is 5! (120) permutations
        // uses recursion and the helper method swap

        // Base case: if we've fixed all positions, add the route (home -> permutation
        // -> home)
        if (start == cities.length) {
            // Create a new route array with home at start and end
            String[] route = new String[cities.length + 2];
            route[0] = home; // Start at home airport
            System.arraycopy(cities, 0, route, 1, cities.length); // Add current permutation
            route[route.length - 1] = home; // End at home airport
            routes.add(route); // Add this route to the list
        } else {
            // Recursively generate all permutations by swapping each element into the
            // current position
            for (int i = start; i < cities.length; i++) {
                swap(cities, start, i); // Swap to fix one city at the current position
                permuteRoutes(cities, start + 1, routes, home); // Recurse for the next position
                swap(cities, start, i); // Backtrack: undo the swap
            }
        }
    }

    // Helper method to swap elements in array
    private static void swap(String[] arr, int i, int j) {
        String temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
