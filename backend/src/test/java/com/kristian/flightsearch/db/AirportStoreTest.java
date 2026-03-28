package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.models.Airport;

@DisplayName("AirportStore Tests")
class AirportStoreTest {

    private static boolean dbAvailable = false;
    private static AirportStore airportStore;

    @BeforeAll
    static void setUpDatabase() {
        try {
            DatabaseManager.initialize();
            airportStore = new AirportStore(DatabaseManager.getDataSource());
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
    @DisplayName("getAirports() returns a non-empty array when the DB has data")
    void testGetAirportsReturnsData() {
        Airport[] airports = airportStore.getAirports();
        assertTrue(airports.length > 0);
    }

    @Test
    @DisplayName("getAirportByCode() returns null for an unknown code")
    void testGetAirportByCodeUnknown() {
        assertNull(airportStore.getAirportByCode("ZZZ"));
    }

    @Test
    @DisplayName("isValidAirportCode() returns false for blank input")
    void testIsValidAirportCodeBlank() {
        assertFalse(airportStore.isValidAirportCode(""));
    }

    @Test
    @DisplayName("isValidAirportCode() returns false for null input")
    void testIsValidAirportCodeNull() {
        assertFalse(airportStore.isValidAirportCode(null));
    }
}
