package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Flight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DisplayName("FlightStore Tests")
class FlightStoreTest {

    private static boolean dbAvailable = false;
    private static FlightStore flightStore;

    @BeforeAll
    static void setUpDatabase() {
        try {
            DatabaseManager.initialize();
            AirportStore airportStore = new AirportStore(DatabaseManager.getDataSource());
            flightStore = new FlightStore(DatabaseManager.getDataSource(), airportStore);
            dbAvailable = true;
        } catch (Exception e) {
            dbAvailable = false;
        }
    }

    @BeforeEach
    void assumeDatabase() {
        assumeTrue(dbAvailable, "Database not available — skipping test");
    }

    @Test
    @DisplayName("readFlights() returns a non-empty map when flights exist")
    void testReadFlightsReturnsData() {
        HashMap<String, Flight> flights = flightStore.readFlights();
        assertTrue(flights.size() > 0);
    }

    @Test
    @DisplayName("readFlights() returns flights with non-null origin and destination")
    void testReadFlightsHaveAirports() {
        HashMap<String, Flight> flights = flightStore.readFlights();
        assumeTrue(!flights.isEmpty(), "No flights in database — skipping test");

        Flight sample = flights.values().iterator().next();
        assertNotNull(sample.getOrigin());
        assertNotNull(sample.getDestination());
    }

    @Test
    @DisplayName("readFlightsForBoard() returns a map with departures and arrivals keys")
    void testReadFlightsForBoardReturnsExpectedKeys() {
        Map<String, List<Map<String, Object>>> board = flightStore.readFlightsForBoard("LHR");
        assertTrue(board.containsKey("departures"));
        assertTrue(board.containsKey("arrivals"));
    }

    @Test
    @DisplayName("readFlightsForBoard() departures all have origin matching queried airport")
    void testReadFlightsForBoardDeparturesOriginMatchesAirport() {
        Map<String, List<Map<String, Object>>> board = flightStore.readFlightsForBoard("LHR");
        List<Map<String, Object>> departures = board.get("departures");
        assumeTrue(!departures.isEmpty(), "No departures found for LHR — skipping test");
        for (Map<String, Object> flight : departures) {
            assertEquals("LHR", flight.get("origin"));
        }
    }

    @Test
    @DisplayName("readFlightsForBoard() arrivals all have destination matching queried airport")
    void testReadFlightsForBoardArrivalsDestinationMatchesAirport() {
        Map<String, List<Map<String, Object>>> board = flightStore.readFlightsForBoard("LHR");
        List<Map<String, Object>> arrivals = board.get("arrivals");
        assumeTrue(!arrivals.isEmpty(), "No arrivals found for LHR — skipping test");
        for (Map<String, Object> flight : arrivals) {
            assertEquals("LHR", flight.get("destination"));
        }
    }

    @Test
    @DisplayName("readFlightsForBoard() flight entries contain all expected fields")
    void testReadFlightsForBoardFlightEntryFields() {
        Map<String, List<Map<String, Object>>> board = flightStore.readFlightsForBoard("LHR");
        List<Map<String, Object>> departures = board.get("departures");
        assumeTrue(!departures.isEmpty(), "No departures found for LHR — skipping test");

        Map<String, Object> sample = departures.get(0);
        List<String> expectedFields = List.of(
            "flightNumber", "airlineName", "aircraftName",
            "origin", "originCity", "destination", "destinationCity",
            "departureTime", "arrivalTime", "durationMinutes", "price"
        );
        for (String field : expectedFields) {
            assertTrue(sample.containsKey(field), "Missing field: " + field);
        }
    }
}
