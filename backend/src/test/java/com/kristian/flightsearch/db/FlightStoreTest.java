package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.LegQuery;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                List.of(new LegQuery("AAA", "BBB", LocalDate.of(2026, 4, 1))));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getFlightsForRoute() returns empty list when no flights match")
    void testGetFlightsForRouteReturnsEmptyForNoMatch() {
        ArrayList<Flight> flights = flightStore.getFlightsForRoute("AAA", "BBB", LocalDate.of(2026, 4, 1));
        assertTrue(flights.isEmpty());
    }
}
