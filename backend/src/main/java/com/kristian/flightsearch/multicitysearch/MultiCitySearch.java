package com.kristian.flightsearch.multicitysearch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.FlightStore;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.Edge;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.LegQuery;
import com.kristian.flightsearch.models.Route;

/*
 * Finds valid multi-city routes and sorts them by cheapest total price or shortest duration.
 * Given a home airport and a set of destinations, it generates all possible
 * orderings and filters out any where a direct flight doesn't exist for every leg.
 *
 * Steps:
 *   1) Generate all permutations of the destination airports
 *   2) Remove permutations where any leg has no available direct flight (via connectionSet)
 *   3) For each valid permutation, collect the available flights per leg from the DB
 *   4) Sort results by the requested criterion
 */
public class MultiCitySearch {

    private final AirportStore airportStore;
    // Keyed by "ORIGINDEST" (e.g. "JFKLHR") — used by search() (test/legacy path).
    // Empty when constructed from a FlightGraph.
    final HashMap<String, ArrayList<Flight>> flightIndex;
    // Set of "ORIGINDEST" keys for O(1) direct-flight connectivity checks.
    private final Set<String> connectionSet;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /** Test constructor: connectivity derived from the provided flight index. */
    public MultiCitySearch(AirportStore airportStore, HashMap<String, ArrayList<Flight>> flightIndex) {
        this.airportStore  = airportStore;
        this.flightIndex   = flightIndex;
        this.connectionSet = new HashSet<>(flightIndex.keySet());
    }

    /** Production constructor: connectivity derived from the pre-built graph. */
    public MultiCitySearch(AirportStore airportStore, FlightGraph flightGraph) {
        this.airportStore = airportStore;
        this.flightIndex  = new HashMap<>();
        this.connectionSet = new HashSet<>();
        for (AirportVertex v : flightGraph.getVertices()) {
            for (Edge e : v.getEdges()) {
                connectionSet.add(v.getData().getCode() + e.getEnd().getData().getCode());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dijkstra-based static search (used by tests / legacy callers)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static ArrayList<Route> dijkstraFlightSearch(String homeAirport, String[] destinations, FlightGraph flightNetwork, HashMap<String, ArrayList<Flight>> flightIndex) {
        ArrayList<String[]> combinations = flightCombinations(destinations, homeAirport);

        ArrayList<Route> validRoutes = new ArrayList<>();

        for (String[] permutation : combinations) {
            ArrayList<String> expandedAirports = new ArrayList<>();
            ArrayList<ArrayList<Flight>> flightsPerSubLeg = new ArrayList<>();
            boolean routeValid = true;

            for (int i = 0; i < permutation.length - 1; i++) {
                String originCode = permutation[i];
                String destCode = permutation[i + 1];

                AirportVertex originVertex = flightNetwork.getVertex(originCode);
                AirportVertex destVertex = flightNetwork.getVertex(destCode);

                if (originVertex == null || destVertex == null) {
                    routeValid = false;
                    break;
                }

                Map[] dijkstraResult = Dijkstra.searchByPrice(flightNetwork, originVertex);
                Map<Airport, Integer> prices = (Map<Airport, Integer>) dijkstraResult[0];
                Map<Airport, AirportVertex> previous = (Map<Airport, AirportVertex>) dijkstraResult[1];

                Integer priceToDestination = prices.get(destVertex.getData());
                if (priceToDestination == null || priceToDestination == Integer.MAX_VALUE) {
                    routeValid = false;
                    break;
                }

                ArrayList<String> legPath = new ArrayList<>();
                Airport current = destVertex.getData();
                while (current != null) {
                    legPath.add(0, current.getCode());
                    AirportVertex prevVertex = previous.get(current);
                    current = (prevVertex != null) ? prevVertex.getData() : null;
                }

                int startIndex = expandedAirports.isEmpty() ? 0 : 1;
                for (int j = startIndex; j < legPath.size(); j++) {
                    expandedAirports.add(legPath.get(j));
                }

                for (int j = 0; j < legPath.size() - 1; j++) {
                    String key = legPath.get(j) + legPath.get(j + 1);
                    ArrayList<Flight> subLegFlights = flightIndex.get(key);

                    if (subLegFlights == null || subLegFlights.isEmpty()) {
                        routeValid = false;
                        break;
                    }
                    flightsPerSubLeg.add(subLegFlights);
                }

                if (!routeValid) break;
            }

            if (!routeValid) continue;

            String[] airportsArray = expandedAirports.toArray(new String[0]);
            validRoutes.add(new Route(airportsArray, flightsPerSubLeg));
        }

        validRoutes.sort((a, b) -> Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        return validRoutes;
    }

    // -------------------------------------------------------------------------
    // Non-date search (tests only — uses in-memory flightIndex)
    // -------------------------------------------------------------------------

    /**
     * @param optimizeBy "price" or "duration"
     */
    public ArrayList<Route> search(String homeAirport, String[] destinations, String optimizeBy) {
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
    // Date-specific search
    // -------------------------------------------------------------------------

    /**
     * Searches for valid multi-city routes for specific departure dates,
     * sorted by the given optimizeBy criterion.
     *
     * @param homeAirport    The origin/return airport code
     * @param destinations   Destination airport codes to visit
     * @param departureDate  Date of the first leg
     * @param daysAtAirport  Map from airport code to number of full days spent there
     * @param optimizeBy     "price" or "duration"
     * @param flightStore    Used to fetch date-specific flights from the database
     */
    public ArrayList<Route> searchByDate(String homeAirport, String[] destinations,
            LocalDate departureDate, Map<String, Integer> daysAtAirport,
            String optimizeBy, FlightStore flightStore) {

        ArrayList<String[]> validPerms = filterValidPermutations(destinations, homeAirport);
        if (validPerms.isEmpty()) return new ArrayList<>();

        LinkedHashSet<LegQuery> uniqueLegs = new LinkedHashSet<>();
        for (String[] perm : validPerms) {
            LocalDate[] dates = computeLegDates(perm, departureDate, daysAtAirport);
            for (int i = 0; i < perm.length - 1; i++) {
                uniqueLegs.add(new LegQuery(perm[i], perm[i + 1], dates[i]));
            }
        }

        HashMap<String, ArrayList<Flight>> dateIndex = flightStore.readFlightsForLegs(new ArrayList<>(uniqueLegs));
        return buildRoutesFromDateIndex(validPerms, departureDate, daysAtAirport, dateIndex, optimizeBy);
    }

    /**
     * Same as searchByDate but accepts a pre-built date-keyed flight index instead of
     * querying the database. Used in tests.
     */
    ArrayList<Route> searchByDateWithIndex(String homeAirport, String[] destinations,
            LocalDate departureDate, Map<String, Integer> daysAtAirport,
            String optimizeBy, HashMap<String, ArrayList<Flight>> dateIndex) {

        ArrayList<String[]> validPerms = filterValidPermutations(destinations, homeAirport);
        if (validPerms.isEmpty()) return new ArrayList<>();
        return buildRoutesFromDateIndex(validPerms, departureDate, daysAtAirport, dateIndex, optimizeBy);
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

                if (connectionSet.contains(origin + dest)) {
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

    private record ConnectionResult(
        ArrayList<Flight> validInbounds,
        ArrayList<Flight> validOutbounds,
        int minConnectionMinutes,
        boolean hasOvernight
    ) {}

    // Validates inbound/outbound pairings at a connection point.
    // Same-day: outbound departs > MIN_CONNECTION_MINUTES after inbound arrives.
    // Overnight: outbound departs earlier than inbound arrives (next calendar day),
    //            only when the inbound flight itself does not cross midnight.
    // Returns null if no valid pairings exist.
    private ConnectionResult validateConnectionPoint(
            ArrayList<Flight> inbounds, ArrayList<Flight> outbounds) {

        Set<Flight> validInboundSet = new HashSet<>();
        Set<Flight> validOutboundSet = new HashSet<>();
        int minGap = Integer.MAX_VALUE;
        boolean hasOvernight = false;

        for (Flight f1 : inbounds) {
            int arrivalMin = f1.getArrivalTime().toSecondOfDay() / 60;
            boolean arrivesNextDay = f1.getArrivalTime().isBefore(f1.getDepartureTime());
            for (Flight f2 : outbounds) {
                int gap = f2.getDepartureTime().toSecondOfDay() / 60 - arrivalMin;
                if (gap > MIN_CONNECTION_MINUTES) {
                    validInboundSet.add(f1);
                    validOutboundSet.add(f2);
                    minGap = Math.min(minGap, gap);
                } else if (!arrivesNextDay && gap < 0) {
                    validInboundSet.add(f1);
                    validOutboundSet.add(f2);
                    hasOvernight = true;
                    minGap = Math.min(minGap, gap + 1440);
                }
            }
        }

        if (validInboundSet.isEmpty() || validOutboundSet.isEmpty()) return null;
        return new ConnectionResult(
            new ArrayList<>(validInboundSet),
            new ArrayList<>(validOutboundSet),
            minGap == Integer.MAX_VALUE ? 0 : minGap,
            hasOvernight
        );
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
            int[] minConnectionMinutes = new int[numSubLegs];
            boolean[] isOvernightLeg = new boolean[numSubLegs];
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

                    ConnectionResult validated = validateConnectionPoint(inbounds, outbounds);
                    if (validated == null) { routeValid = false; break; }

                    subLegFlights.add(validated.validInbounds());
                    pendingOutbounds = validated.validOutbounds();
                    minConnectionMinutes[i] = validated.minConnectionMinutes();
                    isOvernightLeg[i] = validated.hasOvernight();
                }
            }

            if (routeValid) {
                Route route = new Route(exp, subLegFlights, ep.intendedAirports(), isConnectionLeg);
                route.setConnectionMetadata(minConnectionMinutes, isOvernightLeg);
                validRoutes.add(route);
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
        all.removeIf(perm -> !hasConnectionsForAllLegs(perm));
        return all;
    }

    private boolean hasConnectionsForAllLegs(String[] route) {
        for (int i = 0; i < route.length - 1; i++) {
            if (!connectionSet.contains(route[i] + route[i + 1])) return false;
        }
        return true;
    }

    private static ArrayList<Route> buildRoutesFromDateIndex(ArrayList<String[]> perms,
            LocalDate departureDate, Map<String, Integer> daysAtAirport,
            HashMap<String, ArrayList<Flight>> dateIndex,
            String optimizeBy) {

        ArrayList<Route> validRoutes = new ArrayList<>();

        for (String[] perm : perms) {
            LocalDate[] dates = computeLegDates(perm, departureDate, daysAtAirport);
            ArrayList<ArrayList<Flight>> routeFlights = new ArrayList<>();
            boolean routeValid = true;

            for (int i = 0; i < perm.length - 1; i++) {
                String key = perm[i] + perm[i + 1] + dates[i].toString();
                ArrayList<Flight> legFlights = dateIndex.get(key);
                if (legFlights == null || legFlights.isEmpty()) {
                    routeValid = false;
                    break;
                }
                routeFlights.add(new ArrayList<>(legFlights));
            }

            if (routeValid) {
                validRoutes.add(new Route(perm, routeFlights));
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
        // NOTE: this method uses a legacy schema reference and is not used in production
        return new HashMap<>();
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
