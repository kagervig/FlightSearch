package com.kristian.flightsearch;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kristian.flightsearch.datagenerator.FileReader;
import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class Server {

    // These are initialized once when the server starts, then reused for all requests
    private static FlightGraph flightNetwork;
    private static FileReader fileReader;
    private static HashMap<String, Flight> flightList;
    private static HashMap<String, ArrayList<Flight>> flightIndex;

    public static void main(String[] args) {
        // Initialize flight data (same as Main.java)
        initializeFlightData();

        // Get port from environment variable (Railway sets this), default to 8080 for local dev
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Create Javalin app with JSON support
        Javalin app = Javalin.create(config -> {
            // Enable CORS so your frontend can call this API
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> rule.anyHost());
            });
        });

        // === ROUTES ===

        // Health check - Railway uses this to know your app is running
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        // List all airports
        app.get("/api/airports", Server::getAirports);

        // Search flights between two airports
        // Example: /api/flights/search?from=JFK&to=LAX
        app.get("/api/flights/search", Server::searchFlights);

        // Find cheapest route using Dijkstra
        // Example: /api/routes/cheapest?from=JFK
        app.get("/api/routes/cheapest", Server::findCheapestRoutes);

        // Start the server
        app.start(port);
        System.out.println("Server started on port " + port);
        System.out.println("Endpoints:");
        System.out.println("  GET /health");
        System.out.println("  GET /api/airports");
        System.out.println("  GET /api/flights/search?from=XXX&to=YYY");
        System.out.println("  GET /api/routes/cheapest?from=XXX");
    }

    /**
     * Initialize flight graph and data - runs once at startup
     */
    private static void initializeFlightData() {
        System.out.println("Initializing flight data...");

        String filepath = "flights.txt";
        flightNetwork = new FlightGraph(true, true);
        ArrayList<AirportVertex> airportVertices = new ArrayList<>();

        fileReader = new FileReader("top100global.txt");
        Airport[] airports = fileReader.getAirports();

        for (Airport a : airports) {
            airportVertices.add(flightNetwork.addVertex(a));
        }

        flightList = FlightReader.readFlights(filepath);
        flightIndex = FlightGenerator.flightMapper(flightList);

        // Add edges to the graph
        for (ArrayList<Flight> flights : flightIndex.values()) {
            for (Flight f : flights) {
                AirportVertex origin = flightNetwork.getVertex(f.getOrigin().getCode());
                AirportVertex dest = flightNetwork.getVertex(f.getDestination().getCode());

                if (origin != null && dest != null) {
                    flightNetwork.addEdge(origin, dest, f.getPrice(), f.getDuration(), f.getFlightNumber());
                }
            }
        }

        System.out.println("Loaded " + airports.length + " airports and " + flightList.size() + " flights");
    }

    /**
     * GET /api/airports
     * Returns list of all airports
     */
    private static void getAirports(Context ctx) {
        Airport[] airports = fileReader.getAirports();

        // Convert to a list of simple maps for JSON output
        List<Map<String, Object>> result = new ArrayList<>();
        for (Airport airport : airports) {
            Map<String, Object> airportData = new HashMap<>();
            airportData.put("code", airport.getCode());
            airportData.put("name", airport.getName());
            airportData.put("latitude", airport.getLat());
            airportData.put("longitude", airport.getLon());
            airportData.put("timezone", airport.getTimeZone());
            result.add(airportData);
        }

        ctx.json(result);
    }

    /**
     * GET /api/flights/search?from=XXX&to=YYY
     * Returns flights between two airports
     */
    private static void searchFlights(Context ctx) {
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");

        // Validate parameters
        if (from == null || to == null) {
            ctx.status(400).json(Map.of("error", "Missing 'from' or 'to' parameter"));
            return;
        }

        from = from.toUpperCase();
        to = to.toUpperCase();

        // Validate airport codes
        if (!fileReader.isValidAirportCode(from)) {
            ctx.status(400).json(Map.of("error", "Invalid origin airport code: " + from));
            return;
        }
        if (!fileReader.isValidAirportCode(to)) {
            ctx.status(400).json(Map.of("error", "Invalid destination airport code: " + to));
            return;
        }

        // Build the route key (same format as FlightGenerator uses)
        String routeKey = from + "-" + to;
        ArrayList<Flight> flights = flightIndex.get(routeKey);

        if (flights == null || flights.isEmpty()) {
            ctx.json(Map.of(
                "from", from,
                "to", to,
                "flights", new ArrayList<>(),
                "message", "No direct flights found"
            ));
            return;
        }

        // Convert flights to JSON-friendly format
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
     * Uses Dijkstra to find cheapest routes from origin to all destinations
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

        AirportVertex originVertex = flightNetwork.getVertex(from);
        Map[] result = Dijkstra.searchByPrice(flightNetwork, originVertex);

        // result[0] = distances (prices) with Airport keys, result[1] = previous vertices
        @SuppressWarnings("unchecked")
        Map<Airport, Integer> distances = result[0];

        // Convert to JSON-friendly format
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
