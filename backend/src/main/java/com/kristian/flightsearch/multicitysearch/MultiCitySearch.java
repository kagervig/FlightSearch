package com.kristian.flightsearch.multicitysearch;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
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

    public MultiCitySearch(AirportStore airportStore, HashMap<String, ArrayList<Flight>> flightIndex) {
        this.airportStore = airportStore;
        this.flightIndex = flightIndex;
    }



    @SuppressWarnings("unchecked")
    public static ArrayList<Route> dijkstraFlightSearch(String homeAirport, String[] destinations, FlightGraph flightNetwork, HashMap<String, ArrayList<Flight>> flightIndex) {
        // Step 1: Generate all permutations of the destinations bookended by home
        // e.g. home=YYZ, destinations=[JFK, LAX] -> [[YYZ,JFK,LAX,YYZ], [YYZ,LAX,JFK,YYZ]]
        ArrayList<String[]> combinations = flightCombinations(destinations, homeAirport);

        ArrayList<Route> validRoutes = new ArrayList<>();

        // Step 2: Try to build a valid route for each permutation
        for (String[] permutation : combinations) {
            // expandedAirports will hold the full path including any layover airports
            // e.g. if YYZ->JFK is only reachable via ORD: [YYZ, ORD, JFK, LAX, YYZ]
            ArrayList<String> expandedAirports = new ArrayList<>();

            // one entry per sub-leg (direct segment between two consecutive airports)
            ArrayList<ArrayList<Flight>> flightsPerSubLeg = new ArrayList<>();

            boolean routeValid = true;

            // Step 3: Process each leg of the permutation (e.g. YYZ->JFK, JFK->LAX, LAX->YYZ)
            for (int i = 0; i < permutation.length - 1; i++) {
                String originCode = permutation[i];
                String destCode = permutation[i + 1];

                AirportVertex originVertex = flightNetwork.getVertex(originCode);
                AirportVertex destVertex = flightNetwork.getVertex(destCode);

                // Either airport is missing from the graph — leg can't be flown
                if (originVertex == null || destVertex == null) {
                    routeValid = false;
                    break;
                }

                // Run Dijkstra once from this leg's origin
                // result[0]: cheapest price from origin to every other airport
                // result[1]: previous vertex for each airport (used to reconstruct the path)
                Map[] dijkstraResult = Dijkstra.searchByPrice(flightNetwork, originVertex);
                Map<Airport, Integer> prices = (Map<Airport, Integer>) dijkstraResult[0];
                Map<Airport, AirportVertex> previous = (Map<Airport, AirportVertex>) dijkstraResult[1];

                // If the destination price is MAX_VALUE, it's unreachable from this origin
                Integer priceToDestination = prices.get(destVertex.getData());
                if (priceToDestination == null || priceToDestination == Integer.MAX_VALUE) {
                    routeValid = false;
                    break;
                }

                // Reconstruct the path for this leg by walking backwards through the previous map
                // e.g. dest=JFK, previous[JFK]=ORD, previous[ORD]=YYZ -> path=[YYZ, ORD, JFK]
                ArrayList<String> legPath = new ArrayList<>();
                Airport current = destVertex.getData();
                while (current != null) {
                    legPath.add(0, current.getCode()); // prepend so the path reads origin->dest
                    AirportVertex prevVertex = previous.get(current);
                    current = (prevVertex != null) ? prevVertex.getData() : null;
                }

                // Append this leg's path to the expanded route
                // Skip the first airport on every leg after the first — it's already the last
                // airport of the previous leg, so we'd duplicate it otherwise
                int startIndex = expandedAirports.isEmpty() ? 0 : 1;
                for (int j = startIndex; j < legPath.size(); j++) {
                    expandedAirports.add(legPath.get(j));
                }

                // Step 4: For each sub-leg in the reconstructed path (e.g. YYZ->ORD, ORD->JFK)
                // look up the available flights from the index
                for (int j = 0; j < legPath.size() - 1; j++) {
                    String key = legPath.get(j) + legPath.get(j + 1);
                    ArrayList<Flight> subLegFlights = flightIndex.get(key);

                    // Dijkstra found a graph path but no matching flights exist in the index
                    if (subLegFlights == null || subLegFlights.isEmpty()) {
                        routeValid = false;
                        break;
                    }
                    flightsPerSubLeg.add(subLegFlights);
                }

                if (!routeValid) break;
            }

            if (!routeValid) continue;

            // Step 5: Build a Route from the fully expanded airport list and sub-leg flights
            String[] airportsArray = expandedAirports.toArray(new String[0]);
            validRoutes.add(new Route(airportsArray, flightsPerSubLeg));
        }

        // Step 6: Sort by cheapest total price
        validRoutes.sort((a, b) -> Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        return validRoutes;
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

    /**
     * Searches for valid multi-city routes sorted by the given optimizeBy criterion.
     * Dates are computed by the caller for display only and are not used for flight lookup.
     *
     * @param homeAirport  The origin/return airport code
     * @param destinations Destination airport codes to visit
     * @param optimizeBy   "price" or "duration"
     */
    public ArrayList<Route> searchByDate(String homeAirport, String[] destinations, String optimizeBy) {
        ArrayList<String[]> validPerms = filterValidPermutations(destinations, homeAirport);
        if (validPerms.isEmpty()) return new ArrayList<>();

        ArrayList<Route> validRoutes = new ArrayList<>();
        for (String[] perm : validPerms) {
            ArrayList<ArrayList<Flight>> routeFlights = new ArrayList<>();
            for (int i = 0; i < perm.length - 1; i++) {
                ArrayList<Flight> legFlights = flightIndex.get(perm[i] + perm[i + 1]);
                routeFlights.add(legFlights != null ? legFlights : new ArrayList<>());
            }
            validRoutes.add(new Route(perm, routeFlights));
        }

        if ("duration".equalsIgnoreCase(optimizeBy)) {
            validRoutes.sort((a, b) -> Long.compare(a.getShortestTotalDurationMinutes(), b.getShortestTotalDurationMinutes()));
        } else {
            validRoutes.sort((a, b) -> Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        }
        return validRoutes;
    }

    // -------------------------------------------------------------------------
    // Connection search
    // -------------------------------------------------------------------------

    // Minimum gap in minutes required between an inbound flight's arrival and
    // the outbound's departure for a same-day connection to be valid.
    static final int MIN_CONNECTION_MINUTES = 120;

    // Maximum number of intermediate airports (layover stops) per intended leg.
    static final int MAX_CONNECTIONS_PER_LEG = 2;

    /**
     * Searches for multi-city routes using Dijkstra to find connecting paths for
     * any leg that has no direct flight. Connection points are validated by time gap:
     * outbound must depart > MIN_CONNECTION_MINUTES after inbound arrives (same-day),
     * or the outbound may depart earlier (overnight connection on the next calendar day).
     */
    public ArrayList<Route> searchByDateWithConnections(
            String homeAirport, String[] destinations,
            String optimizeBy, FlightGraph flightGraph) {

        ArrayList<String[]> allPerms = flightCombinations(destinations, homeAirport);
        List<ExpandedPerm> expandedPerms = expandPermsWithConnections(allPerms, flightGraph);
        if (expandedPerms.isEmpty()) return new ArrayList<>();
        return buildConnectionRoutes(expandedPerms, optimizeBy);
    }

    // Bundles an intended permutation with its Dijkstra-expanded airport list and
    // a mapping from each expanded leg index back to the intended leg index.
    // Example: intended=[JFK,LHR,GYE,JFK], expanded=[JFK,LHR,UIO,GYE,JFK],
    //          legMap=[0,1,1,2] (legs 1 and 2 both belong to intended leg 1: LHR→GYE)
    private record ExpandedPerm(String[] intendedAirports, String[] expandedAirports, int[] legMap) {}

    @SuppressWarnings("unchecked")
    private List<ExpandedPerm> expandPermsWithConnections(
            ArrayList<String[]> perms, FlightGraph flightGraph) {
        List<ExpandedPerm> result = new ArrayList<>();
        for (String[] perm : perms) {
            ArrayList<String> expanded = new ArrayList<>();
            ArrayList<Integer> legMapping = new ArrayList<>();
            boolean permValid = true;

            for (int i = 0; i < perm.length - 1; i++) {
                String origin = perm[i];
                String dest = perm[i + 1];
                ArrayList<String> path;

                if (flightIndex.containsKey(origin + dest)) {
                    path = new ArrayList<>(List.of(origin, dest));
                } else {
                    path = findConnectingPath(origin, dest, flightGraph);
                    if (path == null) { permValid = false; break; }
                }

                // Append path to expanded; skip the first airport after the first leg
                // to avoid duplicating the shared airport between consecutive legs.
                int startIdx = expanded.isEmpty() ? 0 : 1;
                for (int j = startIdx; j < path.size(); j++) expanded.add(path.get(j));
                for (int j = 0; j < path.size() - 1; j++) legMapping.add(i);
            }

            if (permValid) {
                result.add(new ExpandedPerm(
                        perm,
                        expanded.toArray(new String[0]),
                        legMapping.stream().mapToInt(Integer::intValue).toArray()));
            }
        }
        return result;
    }

    // Finds the cheapest connecting path from origin to dest via Dijkstra.
    // Returns null if unreachable or if more than MAX_CONNECTIONS_PER_LEG intermediate
    // airports are required.
    @SuppressWarnings("unchecked")
    private ArrayList<String> findConnectingPath(String origin, String dest, FlightGraph flightGraph) {
        AirportVertex originVertex = flightGraph.getVertex(origin);
        AirportVertex destVertex = flightGraph.getVertex(dest);
        if (originVertex == null || destVertex == null) return null;

        Map[] dijkstraResult = Dijkstra.searchByPrice(flightGraph, originVertex);
        Map<Airport, Integer> prices = (Map<Airport, Integer>) dijkstraResult[0];
        Map<Airport, AirportVertex> previous = (Map<Airport, AirportVertex>) dijkstraResult[1];

        Integer priceToDestination = prices.get(destVertex.getData());
        if (priceToDestination == null || priceToDestination == Integer.MAX_VALUE) return null;

        ArrayList<String> path = new ArrayList<>();
        Airport current = destVertex.getData();
        while (current != null) {
            path.add(0, current.getCode());
            AirportVertex prevVertex = previous.get(current);
            current = (prevVertex != null) ? prevVertex.getData() : null;
        }

        // path.size() - 2 = number of intermediate airports
        if (path.size() - 2 > MAX_CONNECTIONS_PER_LEG) return null;
        return path;
    }

    // Validates a connection point between inbound and outbound flights by time gap.
    // Same-day: outbound must depart > MIN_CONNECTION_MINUTES after inbound arrives.
    // Overnight: inbound lands same day but outbound departs "earlier" (next calendar
    // day). Detected by: !arrivesNextDay && gap <= 0.
    // Invalid: inbound crosses midnight and gap <= 0 (outbound would be day N+2).
    //
    // Returns [validInbounds, validOutbounds] for all valid pairings, or null if none.
    @SuppressWarnings("unchecked")
    private ArrayList<Flight>[] validateConnectionPoint(
            ArrayList<Flight> inbounds, ArrayList<Flight> outbounds) {

        Set<Flight> validInboundSet = new HashSet<>();
        Set<Flight> validOutboundSet = new HashSet<>();

        for (Flight f1 : inbounds) {
            int arrivalMin = f1.getArrivalTime().toSecondOfDay() / 60;
            // Arrival time before departure time means the flight crosses midnight.
            boolean arrivesNextDay = f1.getArrivalTime().isBefore(f1.getDepartureTime());

            for (Flight f2 : outbounds) {
                int gap = f2.getDepartureTime().toSecondOfDay() / 60 - arrivalMin;
                boolean sameDayValid = gap > MIN_CONNECTION_MINUTES;
                boolean overnightValid = !arrivesNextDay && gap <= 0;
                if (sameDayValid || overnightValid) {
                    validInboundSet.add(f1);
                    validOutboundSet.add(f2);
                }
            }
        }

        if (validInboundSet.isEmpty() || validOutboundSet.isEmpty()) return null;

        ArrayList<Flight>[] result = new ArrayList[2];
        result[0] = new ArrayList<>(validInboundSet);
        result[1] = new ArrayList<>(validOutboundSet);
        return result;
    }

    private ArrayList<Route> buildConnectionRoutes(
            List<ExpandedPerm> expandedPerms, String optimizeBy) {

        ArrayList<Route> validRoutes = new ArrayList<>();

        for (ExpandedPerm ep : expandedPerms) {
            String[] exp = ep.expandedAirports();
            int[] legMap = ep.legMap();
            int numSubLegs = exp.length - 1;

            ArrayList<ArrayList<Flight>> subLegFlights = new ArrayList<>();
            boolean[] isConnectionLeg = new boolean[numSubLegs];
            boolean routeValid = true;
            ArrayList<Flight> pendingOutbounds = null;

            for (int i = 0; i < numSubLegs; i++) {
                int intendedIdx = legMap[i];
                boolean isConnectionSubLeg = !exp[i + 1].equals(
                        ep.intendedAirports()[intendedIdx + 1]);
                isConnectionLeg[i] = isConnectionSubLeg;

                if (!isConnectionSubLeg) {
                    ArrayList<Flight> flights = pendingOutbounds != null
                            ? pendingOutbounds
                            : flightIndex.getOrDefault(exp[i] + exp[i + 1], new ArrayList<>());
                    if (flights.isEmpty()) { routeValid = false; break; }
                    subLegFlights.add(flights);
                    pendingOutbounds = null;
                } else {
                    ArrayList<Flight> inbounds = pendingOutbounds != null
                            ? pendingOutbounds
                            : flightIndex.getOrDefault(exp[i] + exp[i + 1], new ArrayList<>());
                    if (inbounds.isEmpty()) { routeValid = false; break; }

                    ArrayList<Flight> outbounds = flightIndex.getOrDefault(
                            exp[i + 1] + exp[i + 2], new ArrayList<>());

                    @SuppressWarnings("unchecked")
                    ArrayList<Flight>[] validated = validateConnectionPoint(inbounds, outbounds);
                    if (validated == null) { routeValid = false; break; }

                    subLegFlights.add(validated[0]);
                    pendingOutbounds = validated[1];
                }
            }

            if (routeValid) {
                validRoutes.add(new Route(exp, subLegFlights, ep.intendedAirports(), isConnectionLeg));
            }
        }

        if ("duration".equalsIgnoreCase(optimizeBy)) {
            validRoutes.sort((a, b) ->
                    Long.compare(a.getShortestTotalDurationMinutes(), b.getShortestTotalDurationMinutes()));
        } else {
            validRoutes.sort((a, b) ->
                    Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        }
        return validRoutes;
    }

    /**
     * Computes the departure date for each leg of a route permutation.
     * The offset between consecutive legs is daysAtAirport[stopover] + 1,
     * where +1 accounts for the arrival day not counting as a full day.
     *
     * @param airports       Full route array including home at start and end
     * @param departureDate  Date of the first leg
     * @param daysAtAirport  Map from airport code to number of full days spent there
     * @return Array of departure dates, one per leg (length = airports.length - 1)
     */
    public static LocalDate[] computeLegDates(String[] airports, LocalDate departureDate,
            Map<String, Integer> daysAtAirport) {
        LocalDate[] dates = new LocalDate[airports.length - 1];
        LocalDate current = departureDate;
        for (int i = 0; i < airports.length - 1; i++) {
            dates[i] = current;
            if (i < airports.length - 2) {
                int days = daysAtAirport.getOrDefault(airports[i + 1], 0);
                current = current.plusDays(days + 1);
            }
        }
        return dates;
    }

    private ArrayList<String[]> filterValidPermutations(String[] destinations, String homeAirport) {
        ArrayList<String[]> all = flightCombinations(destinations, homeAirport);
        all.removeIf(perm -> !hasFlightsForAllLegs(perm, flightIndex));
        return all;
    }

    public static Route findCheapestRoute(ArrayList<Route> validRoutes) {
        Route cheapestRoute = validRoutes.get(1);
        for (Route r : validRoutes) {
            if (r.getCheapestTotalPrice() < cheapestRoute.getCheapestTotalPrice()) {
                cheapestRoute = r;
            }
        }
        return cheapestRoute;
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
