package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.FlightResult;

import java.sql.Connection;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@DisplayName("RouteStore tests")
class RouteStoreTest {

    private static boolean dbAvailable = false;
    private static Connection conn;

    @BeforeAll
    static void setUpDatabase() {
        try {
            DatabaseManager.initialize();
            conn = DatabaseManager.getDataSource().getConnection();
            dbAvailable = true;
        } catch (Exception e) {
            dbAvailable = false;
        }
    }

    @BeforeEach
    void assumeDatabase() {
        assumeTrue(dbAvailable, "Database not available — skipping test");
    }

    // --- validateAirport ---

    @Test
    @DisplayName("validateAirport() returns true for a known airport code")
    void testValidateAirportKnown() {
        assertTrue(RouteStore.validateAirport("LHR", conn));
    }

    @Test
    @DisplayName("validateAirport() returns false for an unknown airport code")
    void testValidateAirportUnknown() {
        assertFalse(RouteStore.validateAirport("ZZZ", conn));
    }

    @Test
    @DisplayName("validateAirport() returns false for an empty string")
    void testValidateAirportEmpty() {
        assertFalse(RouteStore.validateAirport("", conn));
    }

    // --- getAirport ---

    @Test
    @DisplayName("getAirport() returns a non-null Airport with the correct code")
    void testGetAirportKnown() {
        Airport airport = RouteStore.getAirport("LHR", conn);
        assertNotNull(airport);
        assertEquals("LHR", airport.getCode());
    }

    @Test
    @DisplayName("getAirport() returns null for an unknown code")
    void testGetAirportUnknown() {
        assertNull(RouteStore.getAirport("ZZZ", conn));
    }

    // --- findFlights ---

    @Test
    @DisplayName("findFlights() returns at most 20 results")
    void testFindFlightsMaxResults() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assertTrue(results.size() <= 20);
    }

    @Test
    @DisplayName("findFlights() results all depart from the given origin")
    void testFindFlightsOriginMatches() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        for (FlightResult fr : results) {
            assertEquals("LHR", fr.origin());
        }
    }

    @Test
    @DisplayName("findFlights() results have non-null fields")
    void testFindFlightsFieldsPopulated() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        for (FlightResult fr : results) {
            assertNotNull(fr.flightNumber());
            assertNotNull(fr.origin());
            assertNotNull(fr.destination());
            assertNotNull(fr.airline());
            assertNotNull(fr.departureTime());
            assertTrue(fr.price() > 0);
        }
    }

    @Test
    @DisplayName("findFlights() returns at most one result per destination")
    void testFindFlightsOnePerDestination() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        Set<String> seen = new HashSet<>();
        for (FlightResult fr : results) {
            assertTrue(seen.add(fr.destination()), "Duplicate destination: " + fr.destination());
        }
    }

    @Test
    @DisplayName("findFlights() results are ordered by price ascending")
    void testFindFlightsSortedByPrice() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assumeTrue(results.size() > 1, "Need at least 2 results to check ordering");
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i).price() >= results.get(i - 1).price(),
                "Results not sorted by price at index " + i);
        }
    }

    @Test
    @DisplayName("findFlights() returns an empty list for an unknown origin")
    void testFindFlightsUnknownOrigin() {
        ArrayList<FlightResult> results = RouteStore.findFlights("ZZZ", conn);
        assertTrue(results.isEmpty());
    }

    // --- findFlightHome ---

    @Test
    @DisplayName("findFlightHome() results all depart from currentLocation and arrive at homeAirport")
    void testFindFlightHomeCorrectRoute() {
        ArrayList<FlightResult> outbound = RouteStore.findFlights("LHR", conn);
        assumeTrue(!outbound.isEmpty(), "No flights from LHR — skipping test");

        String stopover = outbound.get(0).destination();
        ArrayList<FlightResult> homeFlights = RouteStore.findFlightHome(stopover, "LHR", conn);
        assumeTrue(!homeFlights.isEmpty(), "No direct flights back to LHR from " + stopover + " — skipping test");

        for (FlightResult fr : homeFlights) {
            assertEquals(stopover, fr.origin());
            assertEquals("LHR", fr.destination());
        }
    }

    @Test
    @DisplayName("findFlightHome() returns an empty list when no direct flight exists")
    void testFindFlightHomeNoRoute() {
        ArrayList<FlightResult> results = RouteStore.findFlightHome("ZZZ", "YYY", conn);
        assertTrue(results.isEmpty());
    }

    // --- convertToFlight ---

    @Test
    @DisplayName("convertToFlight() returns a fully populated Flight for a valid FlightResult")
    void testConvertToFlightValid() {
        ArrayList<FlightResult> results = RouteStore.findFlights("LHR", conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");

        FlightResult fr = results.get(0);
        Flight flight = RouteStore.convertToFlight(fr, conn);

        assertNotNull(flight);
        assertEquals(fr.flightNumber(), flight.getFlightNumber());
        assertEquals(fr.origin(), flight.getOrigin().getCode());
        assertEquals(fr.destination(), flight.getDestination().getCode());
        assertEquals(fr.price(), flight.getPrice());
        assertEquals(fr.airline(), flight.getAirlineName());
    }

    @Test
    @DisplayName("convertToFlight() returns null when origin airport is unknown")
    void testConvertToFlightUnknownOrigin() {
        FlightResult fr = new FlightResult("XX 0000", "ZZZ", "LHR", 100, "XX", LocalTime.of(10, 0));
        assertNull(RouteStore.convertToFlight(fr, conn));
    }

    @Test
    @DisplayName("convertToFlight() returns null when destination airport is unknown")
    void testConvertToFlightUnknownDestination() {
        FlightResult fr = new FlightResult("XX 0000", "LHR", "ZZZ", 100, "XX", LocalTime.of(10, 0));
        assertNull(RouteStore.convertToFlight(fr, conn));
    }
}
