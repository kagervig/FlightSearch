package com.kristian.flightsearch.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Tests for Route's connection minute calculation and overnight detection.
 */
@DisplayName("Route Tests")
class RouteTest {

    private Airport origin;
    private Airport connection;
    private Airport destination;

    @BeforeEach
    void setUp() {
        origin     = new Airport("JFK", "John F. Kennedy", 40.64, -73.78, 14511, 13, "New York", "USA");
        connection = new Airport("PTY", "Tocumen International", 9.07, -79.38, 1500, 50, "Panama City", "Panama");
        destination = new Airport("GYE", "José Joaquín de Olmedo", -2.15, -79.88, 500, 10, "Guayaquil", "Ecuador");
    }

    private Flight flightWith(Airport from, Airport to, LocalTime departure, LocalTime arrival, int price) {
        Flight f = new Flight(from, to, 1000.0, departure, "AA001");
        f.setArrivalTime(arrival);
        f.setPrice(price);
        return f;
    }

    private Route connectionRoute(ArrayList<Flight> inboundFlights, ArrayList<Flight> outboundFlights) {
        String[] airports = {"JFK", "PTY", "GYE"};
        String[] intended = {"JFK", "GYE"};
        boolean[] isConnectionLeg = {true, false};
        ArrayList<ArrayList<Flight>> legs = new ArrayList<>(List.of(inboundFlights, outboundFlights));
        return new Route(airports, legs, intended, isConnectionLeg);
    }

    @Test
    @DisplayName("computeConnectionMinutes returns correct gap for a same-day connection")
    void computeConnectionMinutesSameDayConnection() {
        Flight inbound  = flightWith(origin, connection, LocalTime.of(8, 0), LocalTime.of(10, 0), 100);
        Flight outbound = flightWith(connection, destination, LocalTime.of(13, 0), LocalTime.of(16, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        );

        assertEquals(180, route.computeConnectionMinutes(0));
    }

    @Test
    @DisplayName("computeConnectionMinutes wraps correctly for an overnight connection")
    void computeConnectionMinutesOvernightConnection() {
        // inbound arrives 22:00, outbound departs 02:00 next day → 4 h layover
        Flight inbound  = flightWith(origin, connection, LocalTime.of(19, 0), LocalTime.of(22, 0), 100);
        Flight outbound = flightWith(connection, destination, LocalTime.of(2, 0), LocalTime.of(7, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        );

        assertEquals(240, route.computeConnectionMinutes(0));
    }

    @Test
    @DisplayName("computeConnectionMinutes uses the cheapest inbound flight's arrival time")
    void computeConnectionMinutesPicksCheapestInbound() {
        // cheap arrives 11:00, expensive arrives 10:00 — outbound departs 14:00
        Flight cheap     = flightWith(origin, connection, LocalTime.of(8, 0), LocalTime.of(11, 0), 100);
        Flight expensive = flightWith(origin, connection, LocalTime.of(7, 0), LocalTime.of(10, 0), 300);
        Flight outbound  = flightWith(connection, destination, LocalTime.of(14, 0), LocalTime.of(18, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(cheap, expensive)),
                new ArrayList<>(List.of(outbound))
        );

        assertEquals(180, route.computeConnectionMinutes(0)); // 14:00 - 11:00
    }

    @Test
    @DisplayName("computeConnectionMinutes uses the cheapest outbound flight's departure time")
    void computeConnectionMinutesPicksCheapestOutbound() {
        // inbound arrives 10:00 — cheap outbound departs 13:00, expensive departs 15:00
        Flight inbound   = flightWith(origin, connection, LocalTime.of(7, 0), LocalTime.of(10, 0), 100);
        Flight cheap     = flightWith(connection, destination, LocalTime.of(13, 0), LocalTime.of(17, 0), 100);
        Flight expensive = flightWith(connection, destination, LocalTime.of(15, 0), LocalTime.of(19, 0), 300);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(cheap, expensive))
        );

        assertEquals(180, route.computeConnectionMinutes(0)); // 13:00 - 10:00
    }

    @Test
    @DisplayName("computeConnectionMinutes returns 0 when leg index has no following leg")
    void computeConnectionMinutesOutOfBounds() {
        Flight inbound  = flightWith(origin, connection, LocalTime.of(8, 0), LocalTime.of(10, 0), 100);
        Flight outbound = flightWith(connection, destination, LocalTime.of(13, 0), LocalTime.of(16, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        );

        assertEquals(0, route.computeConnectionMinutes(1));
    }

    @Test
    @DisplayName("isOvernightConnectionLeg returns false for a same-day connection")
    void isOvernightConnectionLegReturnsFalseForSameDay() {
        Flight inbound  = flightWith(origin, connection, LocalTime.of(8, 0), LocalTime.of(10, 0), 100);
        Flight outbound = flightWith(connection, destination, LocalTime.of(13, 0), LocalTime.of(16, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        );

        assertFalse(route.isOvernightConnectionLeg(0));
    }

    @Test
    @DisplayName("isOvernightConnectionLeg returns true when outbound departs before inbound arrives")
    void isOvernightConnectionLegReturnsTrueForOvernight() {
        Flight inbound  = flightWith(origin, connection, LocalTime.of(19, 0), LocalTime.of(22, 0), 100);
        Flight outbound = flightWith(connection, destination, LocalTime.of(2, 0), LocalTime.of(7, 0), 100);

        Route route = connectionRoute(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        );

        assertTrue(route.isOvernightConnectionLeg(0));
    }

    @Test
    @DisplayName("isOvernightConnectionLeg returns false for a non-connection leg")
    void isOvernightConnectionLegReturnsFalseForNonConnectionLeg() {
        Flight inbound  = flightWith(origin, connection, LocalTime.of(19, 0), LocalTime.of(22, 0), 100);
        // outbound departs before inbound arrives, but this leg is not flagged as a connection
        Flight outbound = flightWith(connection, destination, LocalTime.of(2, 0), LocalTime.of(7, 0), 100);

        String[] airports = {"JFK", "PTY", "GYE"};
        String[] intended = {"JFK", "PTY", "GYE"};
        boolean[] isConnectionLeg = {false, false};
        ArrayList<ArrayList<Flight>> legs = new ArrayList<>(List.of(
                new ArrayList<>(List.of(inbound)),
                new ArrayList<>(List.of(outbound))
        ));
        Route route = new Route(airports, legs, intended, isConnectionLeg);

        assertFalse(route.isOvernightConnectionLeg(0));
    }
}
