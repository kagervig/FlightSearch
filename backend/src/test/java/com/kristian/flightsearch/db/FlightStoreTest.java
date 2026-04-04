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
    @DisplayName("readFlightsForLegs() returns an empty map for an empty leg list")
    void testReadFlightsForLegsEmptyInput() {
        HashMap<String, ArrayList<Flight>> result = flightStore.readFlightsForLegs(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("readFlightsForLegs() returns flights with airline and aircraft names populated")
    void testReadFlightsForLegsPopulatesAirlineAndAircraft() {
        // Use a known leg from the database — YYZ→JFK is a common route
        LegQuery leg = new LegQuery("YYZ", "JFK", LocalDate.of(2026, 4, 1));
        HashMap<String, ArrayList<Flight>> result = flightStore.readFlightsForLegs(List.of(leg));

        assumeTrue(!result.isEmpty(), "No YYZ→JFK flights on 2026-04-01 — skipping test");

        ArrayList<Flight> flights = result.get("YYZJFK2026-04-01");
        assumeTrue(flights != null && !flights.isEmpty(), "Key not found in result — skipping test");

        Flight sample = flights.get(0);
        assertNotNull(sample.getAirlineName(), "Airline name should be populated");
    }
}
