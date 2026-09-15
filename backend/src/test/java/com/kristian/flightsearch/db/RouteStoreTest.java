package com.kristian.flightsearch.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.FlightResult;
import com.kristian.flightsearch.models.FlyHomeResult;
import com.kristian.flightsearch.models.RouteSearchResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DisplayName("RouteStore tests")
class RouteStoreTest {

    private static boolean dbAvailable = false;
    private static Connection conn;
    private static FlightGraph graph;

    @BeforeAll
    static void setUpDatabase() {
        try {
            DatabaseManager.initialize();
            conn = DatabaseManager.getDataSource().getConnection();

            AirportStore airportStore = new AirportStore(DatabaseManager.getDataSource());
            Airport[] airports = airportStore.getAirports();
            graph = FlightGraph.initalizeFlightGraph(airports);
            FlightStore flightStore = new FlightStore(DatabaseManager.getDataSource(), airportStore);
            HashMap<String, Flight> flightList = flightStore.readFlights();
            HashMap<String, ArrayList<Flight>> flightIndex = FlightGenerator.flightMapper(flightList);
            FlightGraph.addFlightEdges(graph, flightIndex);

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

    // --- searchFlights (enriched, with exclude) ---

    @Test
    @DisplayName("searchFlights() results include a non-null destination city")
    void testSearchFlightsCityName() {
        List<RouteSearchResult> results = RouteStore.searchFlights("LHR", List.of(), conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        for (RouteSearchResult r : results) {
            assertNotNull(r.destinationCity(), "destinationCity should not be null");
            assertFalse(r.destinationCity().isBlank(), "destinationCity should not be blank");
        }
    }

    @Test
    @DisplayName("searchFlights() results have distanceKm > 0")
    void testSearchFlightsDistance() {
        List<RouteSearchResult> results = RouteStore.searchFlights("LHR", List.of(), conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        for (RouteSearchResult r : results) {
            assertTrue(r.distanceKm() > 0, "distanceKm should be positive");
        }
    }

    @Test
    @DisplayName("searchFlights() results have durationMinutes > 0")
    void testSearchFlightsDuration() {
        List<RouteSearchResult> results = RouteStore.searchFlights("LHR", List.of(), conn);
        assumeTrue(!results.isEmpty(), "No flights from LHR — skipping test");
        for (RouteSearchResult r : results) {
            assertTrue(r.durationMinutes() > 0, "durationMinutes should be positive");
        }
    }

    @Test
    @DisplayName("searchFlights() with exclude list omits excluded destinations")
    void testSearchFlightsExclude() {
        List<RouteSearchResult> all = RouteStore.searchFlights("LHR", List.of(), conn);
        assumeTrue(all.size() >= 2, "Need at least 2 results to test exclusion");
        String excluded = all.get(0).destination();
        List<RouteSearchResult> filtered = RouteStore.searchFlights("LHR", List.of(excluded), conn);
        for (RouteSearchResult r : filtered) {
            assertNotEquals(excluded, r.destination(), "Excluded destination appeared in results");
        }
    }

    @Test
    @DisplayName("searchFlights() with empty exclude list returns at most 20 results sorted by price")
    void testSearchFlightsEmptyExclude() {
        List<RouteSearchResult> results = RouteStore.searchFlights("LHR", List.of(), conn);
        assertTrue(results.size() <= 20);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i).price() >= results.get(i - 1).price(),
                "Results not sorted by price at index " + i);
        }
    }

    @Test
    @DisplayName("searchFlights() returns empty list for unknown origin")
    void testSearchFlightsUnknownOrigin() {
        List<RouteSearchResult> results = RouteStore.searchFlights("ZZZ", List.of(), conn);
        assertTrue(results.isEmpty());
    }

    // --- routeHome ---

    @Test
    @DisplayName("routeHome() returns direct=true when a direct flight exists")
    void testRouteHomeDirect() {
        List<RouteSearchResult> outbound = RouteStore.searchFlights("LHR", List.of(), conn);
        assumeTrue(!outbound.isEmpty(), "No flights from LHR — skipping test");
        String stopover = outbound.get(0).destination();

        // Find a stopover that has a direct flight back to LHR
        String withDirect = null;
        for (RouteSearchResult r : outbound) {
            List<RouteSearchResult> back = RouteStore.searchFlights(r.destination(), List.of(), conn);
            boolean hasDirectToLHR = back.stream().anyMatch(b -> b.destination().equals("LHR"));
            if (hasDirectToLHR) {
                withDirect = r.destination();
                break;
            }
        }
        assumeTrue(withDirect != null, "No LHR outbound destination has a direct flight back — skipping test");

        FlyHomeResult result = RouteStore.routeHome(withDirect, "LHR", graph, conn);
        assertTrue(result.direct());
        assertEquals(1, result.legs().size());
        assertEquals(withDirect, result.legs().get(0).origin());
        assertEquals("LHR", result.legs().get(0).destination());
        assertTrue(result.totalPrice() > 0);
    }

    @Test
    @DisplayName("routeHome() returns empty legs when no path exists in the graph")
    void testRouteHomeNoPath() {
        FlyHomeResult result = RouteStore.routeHome("ZZZ", "YYY", graph, conn);
        assertFalse(result.direct());
        assertTrue(result.legs().isEmpty());
        assertEquals(0, result.totalPrice());
    }

    @Test
    @DisplayName("routeHome() returns a multi-leg Dijkstra route when no direct flight exists")
    void testRouteHomeDijkstraFallback() {
        // Find an airport that has no direct flight to LHR by checking the flights table
        String noDirectOrigin = findOriginWithNoDirectFlight("LHR", conn);
        assumeTrue(noDirectOrigin != null, "Every airport has a direct flight to LHR — skipping Dijkstra fallback test");

        FlyHomeResult result = RouteStore.routeHome(noDirectOrigin, "LHR", graph, conn);
        assertFalse(result.direct());
        assertFalse(result.legs().isEmpty(), "Expected Dijkstra to find a multi-leg route");
        assertTrue(result.totalPrice() > 0);
        assertEquals("LHR", result.legs().get(result.legs().size() - 1).destination());
    }

    /** Returns an IATA code that appears in the graph but has no direct flight to the given home airport, or null if none found. */
    private static String findOriginWithNoDirectFlight(String home, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT a.iata_code" +
                " FROM airports a" +
                " WHERE a.iata_code != ?" +
                "   AND a.iata_code NOT IN (" +
                "       SELECT origin FROM flights WHERE destination = ?" +
                "   )" +
                " LIMIT 1")) {
            ps.setString(1, home);
            ps.setString(2, home);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("iata_code");
        } catch (SQLException e) {
            // fall through
        }
        return null;
    }
}
