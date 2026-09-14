package com.kristian.flightsearch;

/*
 * HTTP-level integration tests for GET /api/flights/board.
 *
 * Because getBoardFlights is private on Server and the stores are private statics,
 * this test starts a minimal Javalin app that replicates the handler's logic using
 * real stores backed by the database. DB availability is guarded with assumeTrue,
 * consistent with FlightStoreTest.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.db.FlightStore;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("GET /api/flights/board")
class ServerBoardTest {

    private static Javalin app;
    private static HttpClient http;
    private static int port;
    private static boolean dbAvailable = false;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void startServer() {
        try {
            DatabaseManager.initialize();
            AirportStore airportStore = new AirportStore(DatabaseManager.getDataSource());
            FlightStore flightStore = new FlightStore(DatabaseManager.getDataSource(), airportStore);
            dbAvailable = true;

            app = Javalin.create().get("/api/flights/board", ctx -> {
                String airport = ctx.queryParam("airport");
                if (airport == null || airport.isBlank()) {
                    ctx.status(400).json(Map.of("error", "Missing 'airport' parameter"));
                    return;
                }
                airport = airport.trim().toUpperCase();
                if (!airportStore.isValidAirportCode(airport)) {
                    ctx.status(400).json(Map.of("error", "Airport not supported: " + airport));
                    return;
                }
                var board = flightStore.readFlightsForBoard(airport);
                ctx.json(Map.of(
                        "airport", airport,
                        "departures", board.get("departures"),
                        "arrivals", board.get("arrivals")));
            }).start(0); // port 0 lets the OS pick a free port

            port = app.port();
            http = HttpClient.newHttpClient();
        } catch (Exception e) {
            dbAvailable = false;
        }
    }

    @AfterAll
    static void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    @BeforeEach
    void assumeDatabase() {
        assumeTrue(dbAvailable, "Database not available — skipping test");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("returns 400 when airport param is absent")
    void testMissingAirportParam() throws Exception {
        var response = get("/api/flights/board");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Missing 'airport' parameter"));
    }

    @Test
    @DisplayName("returns 400 when airport param is blank")
    void testBlankAirportParam() throws Exception {
        var response = get("/api/flights/board?airport=%20%20%20");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Missing 'airport' parameter"));
    }

    @Test
    @DisplayName("returns 400 for an unrecognised airport code")
    void testInvalidAirportCode() throws Exception {
        var response = get("/api/flights/board?airport=ZZZ");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Airport not supported: ZZZ"));
    }

    @Test
    @DisplayName("returns 200 with airport, departures, and arrivals for a valid airport")
    void testValidAirportReturnsBoard() throws Exception {
        var response = get("/api/flights/board?airport=LHR");
        assertEquals(200, response.statusCode());
        Map<?, ?> body = MAPPER.readValue(response.body(), Map.class);
        assertEquals("LHR", body.get("airport"));
        assertTrue(body.containsKey("departures"));
        assertTrue(body.containsKey("arrivals"));
    }

    @Test
    @DisplayName("normalises a lowercase airport code to uppercase in the response")
    void testLowercaseCodeIsNormalised() throws Exception {
        var response = get("/api/flights/board?airport=lhr");
        assertEquals(200, response.statusCode());
        Map<?, ?> body = MAPPER.readValue(response.body(), Map.class);
        assertEquals("LHR", body.get("airport"));
    }

    @Test
    @DisplayName("departures list contains only flights departing from the queried airport")
    void testDeparturesOriginMatchesAirport() throws Exception {
        var response = get("/api/flights/board?airport=LHR");
        assertEquals(200, response.statusCode());
        Map<?, ?> body = MAPPER.readValue(response.body(), Map.class);
        List<?> departures = (List<?>) body.get("departures");
        assumeTrue(!departures.isEmpty(), "No departures found for LHR — skipping test");
        for (Object flight : departures) {
            Map<?, ?> f = (Map<?, ?>) flight;
            assertEquals("LHR", f.get("origin"), "Expected departure origin to be LHR");
        }
    }

    @Test
    @DisplayName("arrivals list contains only flights arriving at the queried airport")
    void testArrivalsDestinationMatchesAirport() throws Exception {
        var response = get("/api/flights/board?airport=LHR");
        assertEquals(200, response.statusCode());
        Map<?, ?> body = MAPPER.readValue(response.body(), Map.class);
        List<?> arrivals = (List<?>) body.get("arrivals");
        assumeTrue(!arrivals.isEmpty(), "No arrivals found for LHR — skipping test");
        for (Object flight : arrivals) {
            Map<?, ?> f = (Map<?, ?>) flight;
            assertEquals("LHR", f.get("destination"), "Expected arrival destination to be LHR");
        }
    }
}
