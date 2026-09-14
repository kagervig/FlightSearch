package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.LegQuery;

import java.util.ArrayList;
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
    @DisplayName("getConnectionMap() returns a non-empty list")
    void testGetConnectionMapReturnsData() {
        List<String[]> connections = flightStore.getConnectionMap();
        assertFalse(connections.isEmpty());
    }

    @Test
    @DisplayName("getConnectionMap() returns entries with two non-null airport codes each")
    void testGetConnectionMapHasValidCodes() {
        List<String[]> connections = flightStore.getConnectionMap();
        assumeTrue(!connections.isEmpty(), "No connections in database — skipping test");

        String[] first = connections.get(0);
        assertEquals(2, first.length);
        assertNotNull(first[0]);
        assertNotNull(first[1]);
    }

    @Test
    @DisplayName("readFlightsForLegs() returns empty map when no legs match")
    void testReadFlightsForLegsReturnsEmptyForNoMatch() {
        HashMap<String, ArrayList<Flight>> result = flightStore.readFlightsForLegs(
                List.of(new LegQuery("AAA", "BBB")));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getFlightsForRoute() returns empty list when no flights match")
    void testGetFlightsForRouteReturnsEmptyForNoMatch() {
        ArrayList<Flight> flights = flightStore.getFlightsForRoute("AAA", "BBB");
        assertTrue(flights.isEmpty());
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
