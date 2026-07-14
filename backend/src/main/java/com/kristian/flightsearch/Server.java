package com.kristian.flightsearch;

/*
 * Server.java - The REST API server for FlightSearch
 *
 * This file creates a web server using Javalin that exposes your flight search
 * functionality as HTTP endpoints. Instead of a command-line menu, users (or a frontend)
 * can make HTTP requests to search for flights.
 *
 * Endpoints:
 *   GET /health                        - Returns {"status":"ok"} if server is running
 *   GET /api/airports                  - Returns list of all airports as JSON
 *   GET /api/flights/search?from=X&to=Y - Returns direct flights between two airports
 *   GET /api/routes/cheapest?from=X    - Uses Dijkstra to find cheapest routes from X
 *
 * How it works:
 *   1. On startup, loads airport data and a lightweight connection map into memory
 *   2. Builds a FlightGraph from the connection map (edges weighted by distance)
 *   3. Listens for HTTP requests and responds with JSON data
 */

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.db.FlightStore;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;
import com.kristian.flightsearch.multicitysearch.MultiCitySearch;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class Server {

    // Static variables hold the data - initialized once at startup, reused for all
    // requests
    private static FlightGraph flightNetwork; // Graph structure: airports connected by direct-flight edges
    private static AirportStore airportStore; // Provides airport lookup by code
    private static FlightStore flightStore;   // Handles database queries for date-specific flights

    private static final RateLimiter MULTICITY_LIMITER = new RateLimiter(2, 60_000);
    private static final RateLimiter DEFAULT_LIMITER   = new RateLimiter(3, 60_000);

    // The DB contains flights for June–July 2026 only
    private static final LocalDate DB_MIN_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate DB_MAX_DATE = LocalDate.of(2026, 7, 30);

    public static void main(String[] args) {
        // Step 1: Load all flight data before starting the server
        initializeFlightData();

        // Step 2: Get port from environment variable
        // Railway sets PORT automatically; we default to 8080 for local development
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Step 3: Create Javalin app with CORS enabled
        // CORS (Cross-Origin Resource Sharing) allows your frontend on Vercel
        // to call this API on Railway - without it, browsers block the request
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> rule.anyHost()); // Allow requests from any domain
            });
        });

        // Reject requests that exceed the per-IP rate limit before they reach any handler.
        // Render sits behind a load balancer, so the real client IP is in X-Forwarded-For.
        app.before(ctx -> {
            String ip = ctx.header("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                ip = ip.split(",")[0].trim();
            } else {
                ip = ctx.ip();
            }

            boolean isMultiCity = ctx.path().equals("/api/flights/multicity");
            RateLimiter limiter = isMultiCity ? MULTICITY_LIMITER : DEFAULT_LIMITER;

            if (!limiter.isAllowed(ip)) {
                ctx.status(429).json(Map.of("error", "Too many requests — please wait a moment and try again"));
                ctx.skipRemainingHandlers();
            }
        });

        // Step 4: Define routes (endpoints)
        // Each route maps a URL pattern to a handler function

        // Health check - Render uses this to know your app is running
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        // List all airports - useful for populating dropdowns in the frontend
        app.get("/api/airports", Server::getAirports);

        // Search flights between two airports
        // Example: /api/flights/search?from=JFK&to=LAX
        app.get("/api/flights/search", Server::searchFlights);

        // Find cheapest route using Dijkstra's algorithm
        // Example: /api/routes/cheapest?from=JFK
        // Returns cheapest price to reach every other airport from the origin
        app.get("/api/routes/cheapest", Server::findCheapestRoutes);

        // Multi-city search: permute destinations and find valid routes with flights
        // per leg
        // Example: /api/flights/multicity?from=YYZ&destinations=JFK,LAX,FCO
        app.get("/api/flights/multicity", Server::searchMultiCity);

        // Search airports by city name (partial, case-insensitive)
        // Example: /api/airports/search?city=london
        app.get("/api/airports/search", Server::searchAirportsByCity);

        // All distinct airport-to-airport connections in the flight graph
        // Used to render the full network on the Route Map page
        app.get("/api/graph/connections", Server::getGraphConnections);

        // Step 5: Start the server
        app.start(port);
        System.out.println("Server started on port " + port);
        System.out.println("Endpoints:");
        System.out.println("  GET /health");
        System.out.println("  GET /api/airports");
        System.out.println("  GET /api/flights/search?from=XXX&to=YYY");
        System.out.println("  GET /api/routes/cheapest?from=XXX");
        System.out.println("  GET /api/flights/multicity?from=XXX&destinations=YYY,ZZZ");
        System.out.println("  GET /api/airports/search?city=XXX");
    }

    /**
     * Loads airport data and builds a connectivity graph - runs once at startup.
     * Flight objects are not loaded into memory; they are fetched from the DB per request.
     */
    private static void initializeFlightData() {
        DatabaseManager.initialize();

        airportStore = new AirportStore(DatabaseManager.getDataSource());

        if (!DatabaseManager.areNewTablesPopulated()) {
            System.out.println("WARNING: Database tables are empty. Run backend/scripts/seed_database.sh to load data.");
            System.exit(1);
        }

        Airport[] airports = airportStore.getAirports();
        flightNetwork = FlightGraph.initalizeFlightGraph(airports);

        flightStore = new FlightStore(DatabaseManager.getDataSource(), airportStore);

        // Build connectivity graph from distinct (origin, destination) pairs.
        // Edges are weighted by distance (km) rather than price, so Dijkstra
        // finds shortest-distance routes as a proxy for cheapest.
        List<String[]> connections = flightStore.getConnectionMap();
        FlightGraph.addConnectionEdges(flightNetwork, connections);

        Runtime rt = Runtime.getRuntime();
        long heapUsedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println("Loaded " + airports.length + " airports and " + connections.size() + " connections — heap: " + heapUsedMB + "MB used / " + rt.maxMemory() / 1024 / 1024 + "MB max");
    }

    

    /**
     * GET /api/graph/connections
     * Returns all airports and all distinct (undirected) connections between them
     * drawn from the flight graph's edge list. Used to render the full route network.
     */
    private static void getGraphConnections(Context ctx) {
        // Canonical key for undirected deduplication: always put the lexically smaller
        // code first so A-B and B-A collapse to the same entry.
        Set<String> seen = new HashSet<>();
        List<Map<String, String>> connections = new ArrayList<>();
        Set<String> usedCodes = new HashSet<>();

        for (AirportVertex vertex : flightNetwork.getVertices()) {
            for (var edge : vertex.getEdges()) {
                String from = edge.getStart().getData().getCode();
                String to   = edge.getEnd().getData().getCode();
                String key  = from.compareTo(to) < 0 ? from + "-" + to : to + "-" + from;

                if (seen.add(key)) {
                    Map<String, String> conn = new HashMap<>();
                    conn.put("from", from);
                    conn.put("to", to);
                    connections.add(conn);
                    usedCodes.add(from);
                    usedCodes.add(to);
                }
            }
        }

        // Only include airports that appear in at least one connection
        List<Map<String, Object>> airports = new ArrayList<>();
        for (AirportVertex vertex : flightNetwork.getVertices()) {
            Airport a = vertex.getData();
            if (!usedCodes.contains(a.getCode())) continue;
            Map<String, Object> entry = new HashMap<>();
            entry.put("code",    a.getCode());
            entry.put("city",    a.getCity());
            entry.put("country", a.getCountry());
            entry.put("lat",     a.getLat());
            entry.put("lon",     a.getLon());
            airports.add(entry);
        }

        ctx.json(Map.of("airports", airports, "connections", connections));
    }

    /**
     * GET /api/airports
     * Returns a JSON array of all airports
     *
     * Example response:
     * [
     * {"code": "JFK", "name": "John F. Kennedy International Airport", "latitude":
     * 40.6413, ...},
     * {"code": "LAX", "name": "Los Angeles International Airport", ...},
     * ...
     * ]
     */
    private static void getAirports(Context ctx) {
        Airport[] airports = airportStore.getAirports();

        // Convert Airport objects to Maps for JSON serialization
        // We do this manually to control exactly what fields are included
        List<Map<String, Object>> result = new ArrayList<>();
        for (Airport airport : airports) {
            Map<String, Object> airportData = new HashMap<>();
            airportData.put("code", airport.getCode());
            airportData.put("name", airport.getName());
            airportData.put("latitude", airport.getLat());
            airportData.put("longitude", airport.getLon());
            airportData.put("elevation", airport.getElevation());
            airportData.put("runwayLengthFt", airport.getRunwayLengthFt());
            airportData.put("city", airport.getCity());
            airportData.put("country", airport.getCountry());
            result.add(airportData);
        }

        // ctx.json() converts the List to JSON and sends it as the response
        ctx.json(result);
    }

    /**
     * GET /api/airports/search?city=XXX
     * Returns airports whose city matches the query (case-insensitive, partial match).
     * Returns an empty array if no airports match.
     */
    private static void searchAirportsByCity(Context ctx) {
        String city = ctx.queryParam("city");

        if (city == null || city.isBlank()) {
            ctx.status(400).json(Map.of("error", "Missing or blank 'city' parameter"));
            return;
        }

        Airport[] airports = airportStore.searchByCity(city);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Airport airport : airports) {
            Map<String, Object> airportData = new HashMap<>();
            airportData.put("code", airport.getCode());
            airportData.put("name", airport.getName());
            airportData.put("latitude", airport.getLat());
            airportData.put("longitude", airport.getLon());
            airportData.put("elevation", airport.getElevation());
            airportData.put("runwayLengthFt", airport.getRunwayLengthFt());
            airportData.put("city", airport.getCity());
            airportData.put("country", airport.getCountry());
            result.add(airportData);
        }

        ctx.json(result);
    }

    /**
     * GET /api/flights/search?from=XXX&to=YYY
     * Returns direct flights between two airports
     *
     * Query parameters:
     * from - origin airport code (e.g., "JFK")
     * to - destination airport code (e.g., "LAX")
     *
     * Example response:
     * {
     * "from": "JFK",
     * "to": "LAX",
     * "flights": [
     * {"flightNumber": "AA 1234", "price": 350, "durationMinutes": 330, ...},
     * ...
     * ]
     * }
     */
    private static void searchFlights(Context ctx) {
        // Get query parameters from URL
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");
        String sortBy = ctx.queryParam("sortBy");

        // Validate that both parameters were provided
        if (from == null || to == null) {
            ctx.status(400).json(Map.of("error", "Missing 'from' or 'to' parameter"));
            return;
        }

        // Normalize to uppercase (airport codes are always uppercase)
        from = from.toUpperCase();
        to = to.toUpperCase();

        // Validate that the airport codes exist in our data
        if (!airportStore.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Invalid origin airport code: " + from));
            return;
        }
        if (!airportStore.isValidAirportCode(to)) {
            ctx.status(400).json(Map.of("error", "Invalid destination airport code: " + to));
            return;
        }

        ArrayList<Flight> flights = flightStore.getFlightsForRoute(from, to, DB_MIN_DATE);

        // Handle case where no direct flights exist
        if (flights == null || flights.isEmpty()) {
            ctx.json(Map.of(
                    "from", from,
                    "to", to,
                    "flights", new ArrayList<>(),
                    "message", "No direct flights found"));
            return;
        }

        if ("duration".equalsIgnoreCase(sortBy)) {
            flights.sort(Comparator.comparingLong(f -> f.getDuration().toMinutes()));
        } else {
            flights.sort(Comparator.comparingInt(Flight::getPrice));
        }

        // Convert Flight objects to JSON-friendly Maps
        List<Map<String, Object>> flightData = new ArrayList<>();
        for (Flight f : flights) {
            Map<String, Object> fd = new HashMap<>();
            fd.put("flightNumber", f.getFlightNumber());
            fd.put("from", f.getOrigin().getCode());
            fd.put("to", f.getDestination().getCode());
            fd.put("departureTime", f.getDepartureTime().toString());
            fd.put("arrivalTime", f.getArrivalTime().toString());
            fd.put("durationMinutes", f.getDuration().toMinutes());
            fd.put("price", f.getPrice());
            fd.put("distance", f.getDistance());
            flightData.add(fd);
        }

        ctx.json(Map.of(
                "from", from,
                "to", to,
                "flights", flightData));
    }

    /**
     * GET /api/routes/cheapest?from=XXX
     * Uses Dijkstra's algorithm to find the cheapest route from origin to ALL other
     * airports
     *
     * This is the main feature! Dijkstra's algorithm explores the graph to find
     * the shortest (cheapest) path from one vertex to every other vertex.
     *
     * Query parameters:
     * from - origin airport code (e.g., "JFK")
     *
     * Example response:
     * {
     * "from": "JFK",
     * "routes": [
     * {"destination": "LAX", "destinationName": "Los Angeles International
     * Airport", "cheapestPrice": 350},
     * {"destination": "ORD", "destinationName": "O'Hare International Airport",
     * "cheapestPrice": 180},
     * ...
     * ]
     * }
     */
    private static void findCheapestRoutes(Context ctx) {
        String from = ctx.queryParam("from");
        String sortBy = ctx.queryParam("sortBy");

        if (from == null) {
            ctx.status(400).json(Map.of("error", "Missing 'from' parameter"));
            return;
        }

        from = from.toUpperCase();

        if (!airportStore.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Invalid airport code: " + from));
            return;
        }

        AirportVertex originVertex = flightNetwork.getVertex(from);

        List<Map<String, Object>> routes = new ArrayList<>();

        if ("duration".equalsIgnoreCase(sortBy)) {
            @SuppressWarnings("unchecked")
            Map<Airport, java.time.Duration> durations = Dijkstra.searchByDuration(flightNetwork, originVertex)[0];
            for (Map.Entry<Airport, java.time.Duration> entry : durations.entrySet()) {
                // Filter out unreachable airports (Duration.ofHours(99)) and origin (Duration.ZERO)
                if (entry.getValue() != null && entry.getValue().toMinutes() > 0
                        && entry.getValue().compareTo(java.time.Duration.ofHours(99)) < 0) {
                    Map<String, Object> route = new HashMap<>();
                    route.put("destination", entry.getKey().getCode());
                    route.put("destinationName", entry.getKey().getName());
                    route.put("cheapestDurationMinutes", entry.getValue().toMinutes());
                    routes.add(route);
                }
            }
        } else {
            // Run Dijkstra's algorithm - returns two Maps:
            // result[0] = Map<Airport, Integer> - cheapest price to reach each airport
            // result[1] = Map<Airport, AirportVertex> - previous vertex in path (for
            // reconstructing route)
            @SuppressWarnings("unchecked")
            Map<Airport, Integer> distances = Dijkstra.searchByPrice(flightNetwork, originVertex)[0];
            // Filter out unreachable airports (price = MAX_VALUE) and the origin itself
            // (price = 0)
            for (Map.Entry<Airport, Integer> entry : distances.entrySet()) {
                if (entry.getValue() != null && entry.getValue() != Integer.MAX_VALUE) {
                    Map<String, Object> route = new HashMap<>();
                    route.put("destination", entry.getKey().getCode());
                    route.put("destinationName", entry.getKey().getName());
                    route.put("cheapestPrice", entry.getValue());
                    routes.add(route);
                }
            }
        }

        ctx.json(Map.of(
                "from", from,
                "routes", routes));
    }

    /**
     * GET /api/flights/multicity?from=YYZ&destinations=JFK,LAX&departureDate=2026-04-15&daysAtEachDestination=3,4&optimizeBy=price
     * Permutes destination order, finds valid routes on the specified dates, and returns flights per leg.
     */
    private static void searchMultiCity(Context ctx) {
        String from = ctx.queryParam("from");
        String destinationsParam = ctx.queryParam("destinations");
        String departureDateParam = ctx.queryParam("departureDate");
        String daysParam = ctx.queryParam("daysAtEachDestination");
        String optimizeBy = ctx.queryParam("optimizeBy");

        if (from == null) {
            ctx.status(400).json(Map.of("error", "Please add a home airport"));
            return;
        }
        if (destinationsParam == null) {
            ctx.status(400).json(Map.of("error", "Please enter a destination airport"));
            return;
        }
        if (departureDateParam == null) {
            ctx.status(400).json(Map.of("error", "Please enter a departure date"));
            return;
        }
        if (daysParam == null) {
            ctx.status(400).json(Map.of("error", "Please enter days to spend at each destination"));
            return;
        }

        from = from.trim().toUpperCase();

        if (!airportStore.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Airport not supported: " + from));
            return;
        }

        String[] destinations = destinationsParam.split(",");
        for (int i = 0; i < destinations.length; i++) {
            destinations[i] = destinations[i].trim().toUpperCase();
        }

        if (destinations.length < 1 || destinations.length > 5) {
            ctx.status(400).json(Map.of("error", "Must have between 1 and 5 destinations"));
            return;
        }

        for (String dest : destinations) {
            if (!airportStore.isValidAirportCode(dest)) {
                ctx.status(400).json(Map.of("error", "Airport not supported: " + dest));
                return;
            }
        }

        LocalDate departureDate;
        try {
            departureDate = LocalDate.parse(departureDateParam.trim());
        } catch (DateTimeParseException e) {
            ctx.status(400).json(Map.of("error", "Invalid departure date format — use YYYY-MM-DD"));
            return;
        }

        String[] dayTokens = daysParam.split(",");
        if (dayTokens.length != destinations.length) {
            ctx.status(400).json(Map.of("error",
                    "daysAtEachDestination must have one value per destination (" + destinations.length + " expected)"));
            return;
        }

        Map<String, Integer> daysAtAirport = new HashMap<>();
        for (int i = 0; i < destinations.length; i++) {
            int days;
            try {
                days = Integer.parseInt(dayTokens[i].trim());
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "daysAtEachDestination values must be integers"));
                return;
            }
            if (days < 1) {
                ctx.status(400).json(Map.of("error", "Days at each destination must be at least 1"));
                return;
            }
            daysAtAirport.put(destinations[i], days);
        }

        if (departureDate.isBefore(DB_MIN_DATE) || departureDate.isAfter(DB_MAX_DATE)) {
            ctx.status(400).json(Map.of("error",
                    "Departure date must be between " + DB_MIN_DATE + " and " + DB_MAX_DATE));
            return;
        }

        int totalDays = daysAtAirport.values().stream().mapToInt(Integer::intValue).sum() + destinations.length;
        LocalDate latestDate = departureDate.plusDays(totalDays);
        if (latestDate.isAfter(DB_MAX_DATE)) {
            ctx.status(400).json(Map.of("error",
                    "Trip extends beyond available data — last flight date would be " + latestDate +
                    " but data only goes to " + DB_MAX_DATE));
            return;
        }

        if (optimizeBy == null || (!optimizeBy.equalsIgnoreCase("price") && !optimizeBy.equalsIgnoreCase("duration"))) {
            optimizeBy = "price";
        }

        MultiCitySearch multiCitySearch = new MultiCitySearch(airportStore, flightNetwork);
        ArrayList<Route> validRoutes = multiCitySearch.searchByDate(
                from, destinations, departureDate, daysAtAirport, optimizeBy, flightStore);

        // When no direct-flight routes exist, fall back to connection search via Dijkstra
        if (validRoutes.isEmpty()) {
            validRoutes = multiCitySearch.searchByDateWithConnections(
                    from, destinations, departureDate, daysAtAirport, optimizeBy,
                    flightStore, flightNetwork);
        }

        if (validRoutes.isEmpty()) {
            ctx.json(Map.of("from", from, "routes", new ArrayList<>()));
            return;
        }

        List<Map<String, Object>> routeData = new ArrayList<>();
        for (Route route : validRoutes) {
            Map<String, Object> routeMap = new HashMap<>();
            routeMap.put("airports", route.getAirports());
            routeMap.put("cheapestTotalPrice", route.getCheapestTotalPrice());
            routeMap.put("shortestTotalDurationMinutes", route.getShortestTotalDurationMinutes());
            routeMap.put("hasConnections", route.hasConnections());

            List<Map<String, Object>> legs = new ArrayList<>();
            String[] airports = route.getAirports();
            ArrayList<ArrayList<Flight>> allFlights = route.getFlights();

            // For routes with connections, legDates are stored on the route; for direct-only
            // routes they are computed from the intended airports and daysAtAirport.
            LocalDate[] legDates = route.getLegDates() != null
                    ? route.getLegDates()
                    : MultiCitySearch.computeLegDates(
                            route.getIntendedAirports(), departureDate, daysAtAirport);

            for (int i = 0; i < allFlights.size(); i++) {
                Map<String, Object> leg = new HashMap<>();
                leg.put("from", airports[i]);
                leg.put("to", airports[i + 1]);
                leg.put("date", legDates[i].toString());
                leg.put("isConnection", route.isConnectionLeg(i));
                if (route.isConnectionLeg(i)) {
                    leg.put("connectionMinutes", route.getMinConnectionMinutes(i));
                    leg.put("isOvernightConnection", route.isOvernightConnectionLeg(i));
                }

                ArrayList<Flight> legFlights = allFlights.get(i);
                // Resolve airport metadata from the store using the leg's airport codes,
                // not from the flight object — flight templates can have wrong cities when
                // flight numbers are shared across routes in the seed data.
                Airport fromAirport = airportStore.getAirportByCode(airports[i]);
                Airport toAirport = airportStore.getAirportByCode(airports[i + 1]);
                leg.put("fromCity", fromAirport != null ? fromAirport.getCity() : "");
                leg.put("fromCountry", fromAirport != null ? fromAirport.getCountry() : "");
                leg.put("fromLat", fromAirport != null ? fromAirport.getLat() : 0.0);
                leg.put("fromLon", fromAirport != null ? fromAirport.getLon() : 0.0);
                leg.put("toCity", toAirport != null ? toAirport.getCity() : "");
                leg.put("toCountry", toAirport != null ? toAirport.getCountry() : "");
                leg.put("toLat", toAirport != null ? toAirport.getLat() : 0.0);
                leg.put("toLon", toAirport != null ? toAirport.getLon() : 0.0);

                int cheapestPrice = Integer.MAX_VALUE;
                for (Flight f : legFlights) {
                    if (f.getPrice() < cheapestPrice) cheapestPrice = f.getPrice();
                }

                List<Map<String, Object>> flightData = new ArrayList<>();
                for (Flight f : legFlights) {
                    Map<String, Object> fd = new HashMap<>();
                    fd.put("flightNumber", f.getFlightNumber());
                    fd.put("price", f.getPrice());
                    fd.put("departureTime", f.getDepartureTime().toString());
                    fd.put("arrivalTime", f.getArrivalTime().toString());
                    fd.put("durationMinutes", f.getDuration().toMinutes());
                    fd.put("cheapest", f.getPrice() == cheapestPrice);
                    fd.put("airlineName", f.getAirlineName());
                    fd.put("aircraftName", f.getAircraftName());
                    flightData.add(fd);
                }
                leg.put("flights", flightData);
                legs.add(leg);
            }
            routeMap.put("legs", legs);
            routeData.add(routeMap);
        }

        ctx.json(Map.of("from", from, "routes", routeData));
    }
}
