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
 *   1. On startup, loads all airport and flight data into memory (same as Main.java did)
 *   2. Builds a FlightGraph with airports as vertices and flights as edges
 *   3. Listens for HTTP requests and responds with JSON data
 */

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kristian.flightsearch.datagenerator.FSFileReader;
import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class Server {

    // Static variables hold the data - initialized once at startup, reused for all requests
    // This is efficient because we don't reload data for every request
    private static FlightGraph flightNetwork;      // Graph structure: airports connected by flights
    private static FSFileReader fileReader;          // Provides airport lookup by code
    private static HashMap<String, Flight> flightList;  // All flights indexed by flight number
    private static HashMap<String, ArrayList<Flight>> flightIndex;  // Flights indexed by route (e.g., "JFK-LAX")

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
                cors.addRule(rule -> rule.anyHost());  // Allow requests from any domain
            });
        });

        // Step 4: Define routes (endpoints)
        // Each route maps a URL pattern to a handler function

        // Health check - Railway uses this to know your app is running
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

        // Step 5: Start the server
        app.start(port);
        System.out.println("Server started on port " + port);
        System.out.println("Endpoints:");
        System.out.println("  GET /health");
        System.out.println("  GET /api/airports");
        System.out.println("  GET /api/flights/search?from=XXX&to=YYY");
        System.out.println("  GET /api/routes/cheapest?from=XXX");
    }

    /**
     * Loads all flight data into memory - runs once at startup
     *
     * This is the same logic as Main.java's main() method, but without the menu.
     * We load everything into static variables so it's available for all requests.
     */
    private static void initializeFlightData() {
        System.out.println("Initializing flight data...");

        // Create an empty weighted, directed graph
        // Weighted = edges have values (price, duration)
        // Directed = JFK->LAX is different from LAX->JFK
        String filepath = "flights.txt";
        flightNetwork = new FlightGraph(true, true);
        ArrayList<AirportVertex> airportVertices = new ArrayList<>();

        // Load airports from file and add them as vertices in the graph
        fileReader = new FSFileReader("top100global.txt");
        Airport[] airports = fileReader.getAirports();

        for (Airport a : airports) {
            airportVertices.add(flightNetwork.addVertex(a));
        }

        // Load flights from file
        flightList = FlightReader.readFlights(filepath, airports);

        // Create an index of flights by route (e.g., "JFK-LAX" -> [flight1, flight2, ...])
        // This makes searching for flights between two airports O(1) instead of O(n)
        flightIndex = FlightGenerator.flightMapper(flightList);

        // Add flights as edges in the graph
        // Each flight becomes an edge connecting two airport vertices
        for (ArrayList<Flight> flights : flightIndex.values()) {
            for (Flight f : flights) {
                AirportVertex origin = flightNetwork.getVertex(f.getOrigin().getCode());
                AirportVertex dest = flightNetwork.getVertex(f.getDestination().getCode());

                if (origin != null && dest != null) {
                    // Edge has: price (weight), duration, and flight number
                    flightNetwork.addEdge(origin, dest, f.getPrice(), f.getDuration(), f.getFlightNumber());
                }
            }
        }

        System.out.println("Loaded " + airports.length + " airports and " + flightList.size() + " flights");
    }

    /**
     * GET /api/airports
     * Returns a JSON array of all airports
     *
     * Example response:
     * [
     *   {"code": "JFK", "name": "John F. Kennedy International Airport", "latitude": 40.6413, ...},
     *   {"code": "LAX", "name": "Los Angeles International Airport", ...},
     *   ...
     * ]
     */
    private static void getAirports(Context ctx) {
        Airport[] airports = fileReader.getAirports();

        // Convert Airport objects to Maps for JSON serialization
        // We do this manually to control exactly what fields are included
        List<Map<String, Object>> result = new ArrayList<>();
        for (Airport airport : airports) {
            Map<String, Object> airportData = new HashMap<>();
            airportData.put("code", airport.getCode());
            airportData.put("name", airport.getName());
            airportData.put("latitude", airport.getLat());
            airportData.put("longitude", airport.getLon());
            airportData.put("timezone", airport.getTimeZone());
            airportData.put("runway length", airport.getRunwayLength());
            airportData.put("city", airport.getCity());
            airportData.put("country", airport.getCountry());
            result.add(airportData);
        }

        // ctx.json() converts the List to JSON and sends it as the response
        ctx.json(result);
    }

    /**
     * GET /api/flights/search?from=XXX&to=YYY
     * Returns direct flights between two airports
     *
     * Query parameters:
     *   from - origin airport code (e.g., "JFK")
     *   to   - destination airport code (e.g., "LAX")
     *
     * Example response:
     * {
     *   "from": "JFK",
     *   "to": "LAX",
     *   "flights": [
     *     {"flightNumber": "AA 1234", "price": 350, "durationMinutes": 330, ...},
     *     ...
     *   ]
     * }
     */
    private static void searchFlights(Context ctx) {
        // Get query parameters from URL
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");

        // Validate that both parameters were provided
        if (from == null || to == null) {
            ctx.status(400).json(Map.of("error", "Missing 'from' or 'to' parameter"));
            return;
        }

        // Normalize to uppercase (airport codes are always uppercase)
        from = from.toUpperCase();
        to = to.toUpperCase();

        // Validate that the airport codes exist in our data
        if (!fileReader.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Invalid origin airport code: " + from));
            return;
        }
        if (!fileReader.isValidAirportCode(to)) {
            ctx.status(400).json(Map.of("error", "Invalid destination airport code: " + to));
            return;
        }

        // Look up flights using our index (O(1) lookup)
        // The key format is "ORIGIN-DESTINATION" (e.g., "JFK-LAX")
        String routeKey = from + "-" + to;
        ArrayList<Flight> flights = flightIndex.get(routeKey);

        // Handle case where no direct flights exist
        if (flights == null || flights.isEmpty()) {
            ctx.json(Map.of(
                "from", from,
                "to", to,
                "flights", new ArrayList<>(),
                "message", "No direct flights found"
            ));
            return;
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
            "flights", flightData
        ));
    }

    /**
     * GET /api/routes/cheapest?from=XXX
     * Uses Dijkstra's algorithm to find the cheapest route from origin to ALL other airports
     *
     * This is the main feature! Dijkstra's algorithm explores the graph to find
     * the shortest (cheapest) path from one vertex to every other vertex.
     *
     * Query parameters:
     *   from - origin airport code (e.g., "JFK")
     *
     * Example response:
     * {
     *   "from": "JFK",
     *   "routes": [
     *     {"destination": "LAX", "destinationName": "Los Angeles International Airport", "cheapestPrice": 350},
     *     {"destination": "ORD", "destinationName": "O'Hare International Airport", "cheapestPrice": 180},
     *     ...
     *   ]
     * }
     */
    private static void findCheapestRoutes(Context ctx) {
        String from = ctx.queryParam("from");

        if (from == null) {
            ctx.status(400).json(Map.of("error", "Missing 'from' parameter"));
            return;
        }

        from = from.toUpperCase();

        if (!fileReader.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Invalid airport code: " + from));
            return;
        }

        // Get the starting vertex for Dijkstra
        AirportVertex originVertex = flightNetwork.getVertex(from);

        // Run Dijkstra's algorithm - returns two Maps:
        // result[0] = Map<Airport, Integer> - cheapest price to reach each airport
        // result[1] = Map<Airport, AirportVertex> - previous vertex in path (for reconstructing route)
        Map[] result = Dijkstra.searchByPrice(flightNetwork, originVertex);

        // We only need the distances (prices) for this endpoint
        @SuppressWarnings("unchecked")
        Map<Airport, Integer> distances = result[0];

        // Convert to JSON-friendly format
        // Filter out unreachable airports (price = MAX_VALUE) and the origin itself (price = 0)
        List<Map<String, Object>> routes = new ArrayList<>();
        for (Map.Entry<Airport, Integer> entry : distances.entrySet()) {
            if (entry.getValue() != null && entry.getValue() != Integer.MAX_VALUE) {
                Map<String, Object> route = new HashMap<>();
                route.put("destination", entry.getKey().getCode());
                route.put("destinationName", entry.getKey().getName());
                route.put("cheapestPrice", entry.getValue());
                routes.add(route);
            }
        }

        ctx.json(Map.of(
            "from", from,
            "routes", routes
        ));
    }
}
