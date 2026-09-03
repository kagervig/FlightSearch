package com.kristian.flightsearch.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;

@DisplayName("FlightResult record tests")
class FlightResultTest {

    private static final LocalTime DEPARTURE = LocalTime.of(10, 30);

    @Test
    @DisplayName("constructor stores all fields correctly")
    void testRecordFields() {
        FlightResult fr = new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", DEPARTURE);
        assertEquals("AA 1234", fr.flightNumber());
        assertEquals("JFK", fr.origin());
        assertEquals("LAX", fr.destination());
        assertEquals(299, fr.price());
        assertEquals("AA", fr.airline());
        assertEquals(DEPARTURE, fr.departureTime());
    }

    @Test
    @DisplayName("two records with identical fields are equal")
    void testEquality() {
        FlightResult a = new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", DEPARTURE);
        FlightResult b = new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", DEPARTURE);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("records with different prices are not equal")
    void testInequalityDifferentPrice() {
        FlightResult a = new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", DEPARTURE);
        FlightResult b = new FlightResult("AA 1234", "JFK", "LAX", 399, "AA", DEPARTURE);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("records with different origins are not equal")
    void testInequalityDifferentOrigin() {
        FlightResult a = new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", DEPARTURE);
        FlightResult b = new FlightResult("AA 1234", "ORD", "LAX", 299, "AA", DEPARTURE);
        assertNotEquals(a, b);
    }
}
