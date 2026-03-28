package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Flight;

import java.util.HashMap;

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
}
