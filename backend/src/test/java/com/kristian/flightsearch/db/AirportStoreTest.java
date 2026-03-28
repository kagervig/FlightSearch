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

    @Test
    @DisplayName("searchByCity() returns LHR first and LGW second for 'london'")
    void testSearchByCityLondonOrdering() {
        Airport[] results = airportStore.searchByCity("london");
        assertTrue(results.length >= 2, "Expected at least 2 results for 'london'");
        assertEquals("LHR", results[0].getCode(), "First result should be LHR");
        assertEquals("LGW", results[1].getCode(), "Second result should be LGW");
    }

    @Test
    @DisplayName("searchByCity() finds airports by IATA code prefix")
    void testSearchByCityIataCode() {
        Airport[] results = airportStore.searchByCity("MUC");
        assertTrue(results.length > 0, "Expected results for 'MUC'");
        assertEquals("MUC", results[0].getCode(), "First result should be MUC");
    }

    @Test
    @DisplayName("searchByCity() does not return cities that only contain the query string")
    void testSearchByCityStartsWithOnly() {
        // 'MUC' should not return Temuco (city starts with 'Tem', not 'MUC')
        Airport[] results = airportStore.searchByCity("MUC");
        for (Airport a : results) {
            assertTrue(
                a.getCity().toUpperCase().startsWith("MUC") || a.getCode().toUpperCase().startsWith("MUC"),
                "Result " + a.getCode() + " (" + a.getCity() + ") should start with 'MUC'"
            );
        }
    }
}
