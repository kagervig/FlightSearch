package com.kristian.flightsearch.multicitysearch;

import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Tests for MultiCitySearch using a pre-built flight index.
 */
@DisplayName("MultiCitySearch Tests")
class MultiCitySearchTest {

    private HashMap<String, ArrayList<Flight>> flightIndex;
    private Airport jfk;
    private Airport lhr;
    private Airport cdg;

    @BeforeEach
    void setUp() {
        jfk = new Airport("JFK", "John F. Kennedy International Airport", 40.6413, -73.7781, 14511, 13, "New York", "United States");
        lhr = new Airport("LHR", "London Heathrow Airport", 51.4706, -0.461941, 12799, 83, "London", "United Kingdom");
        cdg = new Airport("CDG", "Charles de Gaulle Airport", 49.0097, 2.5478, 13829, 392, "Paris", "France");

        Flight jfkLhr = new Flight(jfk, lhr, 5570.0, LocalTime.of(9, 0), "AA100");
        Flight lhrCdg = new Flight(lhr, cdg, 344.0, LocalTime.of(14, 0), "BA302");
        Flight cdgJfk = new Flight(cdg, jfk, 5837.0, LocalTime.of(16, 0), "AF006");
        Flight lhrJfk = new Flight(lhr, jfk, 5570.0, LocalTime.of(11, 0), "AA101");
        Flight jfkCdg = new Flight(jfk, cdg, 5837.0, LocalTime.of(8, 0), "AF007");
        Flight cdgLhr = new Flight(cdg, lhr, 344.0, LocalTime.of(12, 0), "BA303");

        flightIndex = new HashMap<>();
        flightIndex.put("JFKLHR", new ArrayList<>(List.of(jfkLhr)));
        flightIndex.put("LHRCDG", new ArrayList<>(List.of(lhrCdg)));
        flightIndex.put("CDGJFK", new ArrayList<>(List.of(cdgJfk)));
        flightIndex.put("LHRJFK", new ArrayList<>(List.of(lhrJfk)));
        flightIndex.put("JFKCDG", new ArrayList<>(List.of(jfkCdg)));
        flightIndex.put("CDGLHR", new ArrayList<>(List.of(cdgLhr)));

        setUpConnectionData();
    }

    @Test
    @DisplayName("search returns valid routes when flights exist for all legs")
    void searchReturnsRoutesWhenFlightsExist() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "price");
        assertFalse(routes.isEmpty());
    }

    @Test
    @DisplayName("search returns empty list when no flights exist for a leg")
    void searchReturnsEmptyWhenLegMissing() {
        HashMap<String, ArrayList<Flight>> sparseIndex = new HashMap<>();
        sparseIndex.put("JFKLHR", flightIndex.get("JFKLHR"));
        // No return legs — no complete route is possible

        MultiCitySearch mcs = new MultiCitySearch(null, sparseIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR"}, "price");
        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("search returns routes sorted cheapest first when optimizeBy is price")
    void searchReturnsCheapestRouteFirst() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "price");
        assertTrue(routes.size() > 1);
        for (int i = 0; i < routes.size() - 1; i++) {
            assertTrue(routes.get(i).getCheapestTotalPrice() <= routes.get(i + 1).getCheapestTotalPrice());
        }
    }

    @Test
    @DisplayName("search returns routes sorted by shortest duration when optimizeBy is duration")
    void searchReturnsFastestRouteFirst() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "duration");
        assertTrue(routes.size() > 1);
        for (int i = 0; i < routes.size() - 1; i++) {
            assertTrue(routes.get(i).getShortestTotalDurationMinutes()
                    <= routes.get(i + 1).getShortestTotalDurationMinutes());
        }
    }

    @Test
    @DisplayName("search excludes permutations where a leg is missing from the index")
    void searchExcludesPermutationWhenLegMissingFromIndex() {
        // Only includes legs for JFK→LHR→CDG→JFK; the reverse permutation has no flights
        HashMap<String, ArrayList<Flight>> partialIndex = new HashMap<>();
        partialIndex.put("JFKLHR", flightIndex.get("JFKLHR"));
        partialIndex.put("LHRCDG", flightIndex.get("LHRCDG"));
        partialIndex.put("CDGJFK", flightIndex.get("CDGJFK"));

        MultiCitySearch mcs = new MultiCitySearch(null, partialIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "price");

        assertEquals(1, routes.size());
        assertArrayEquals(new String[]{"JFK", "LHR", "CDG", "JFK"}, routes.get(0).getAirports());
    }

    @Test
    @DisplayName("search returns empty list when the flight index is empty")
    void searchReturnsEmptyWhenIndexEmpty() {
        MultiCitySearch mcs = new MultiCitySearch(null, new HashMap<>());
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "price");
        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("single destination produces a round-trip route")
    void singleDestinationProducesRoundTrip() {
        HashMap<String, ArrayList<Flight>> index = new HashMap<>();
        index.put("JFKLHR", flightIndex.get("JFKLHR"));
        index.put("LHRJFK", flightIndex.get("LHRJFK"));

        MultiCitySearch mcs = new MultiCitySearch(null, index);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR"}, "price");

        assertFalse(routes.isEmpty());
        String[] airports = routes.get(0).getAirports();
        assertEquals("JFK", airports[0]);
        assertEquals("LHR", airports[1]);
        assertEquals("JFK", airports[airports.length - 1]);
    }

    @Test
    @DisplayName("all routes in search results start and end at the home airport")
    void searchRoutesStartAndEndAtHome() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"}, "price");

        for (Route route : routes) {
            String[] airports = route.getAirports();
            assertEquals("JFK", airports[0]);
            assertEquals("JFK", airports[airports.length - 1]);
        }
    }

    @Test
    @DisplayName("flightCombinations with one destination returns exactly one route")
    void flightCombinationsOneDestination() {
        ArrayList<String[]> combinations = MultiCitySearch.flightCombinations(new String[]{"LHR"}, "JFK");
        assertEquals(1, combinations.size());
        assertArrayEquals(new String[]{"JFK", "LHR", "JFK"}, combinations.get(0));
    }

    @Test
    @DisplayName("flightCombinations with two destinations returns 2 routes")
    void flightCombinationsTwoDestinations() {
        ArrayList<String[]> combinations = MultiCitySearch.flightCombinations(new String[]{"LHR", "CDG"}, "JFK");
        assertEquals(2, combinations.size());
    }

    @Test
    @DisplayName("flightCombinations with three destinations returns 6 routes")
    void flightCombinationsThreeDestinations() {
        ArrayList<String[]> combinations = MultiCitySearch.flightCombinations(new String[]{"LHR", "CDG", "AMS"}, "JFK");
        assertEquals(6, combinations.size());
    }

    @Test
    @DisplayName("flightCombinations routes all start and end at the home airport")
    void flightCombinationsAllRoutesStartAndEndAtHome() {
        ArrayList<String[]> combinations = MultiCitySearch.flightCombinations(new String[]{"LHR", "CDG"}, "JFK");
        for (String[] route : combinations) {
            assertEquals("JFK", route[0]);
            assertEquals("JFK", route[route.length - 1]);
        }
    }

    @Test
    @DisplayName("flightCombinations routes each contain all destinations exactly once")
    void flightCombinationsRoutesContainAllDestinations() {
        String[] destinations = {"LHR", "CDG"};
        ArrayList<String[]> combinations = MultiCitySearch.flightCombinations(destinations, "JFK");
        for (String[] route : combinations) {
            // route is [home, ...destinations..., home] so interior elements are the destinations
            List<String> interior = new ArrayList<>();
            for (int i = 1; i < route.length - 1; i++) {
                interior.add(route[i]);
            }
            for (String dest : destinations) {
                assertTrue(interior.contains(dest), "Route should contain destination " + dest);
            }
        }
    }

    @Test
    @DisplayName("hasFlightsForAllLegs returns true when every leg exists in the index")
    void hasFlightsForAllLegsReturnsTrueWhenComplete() {
        String[] route = {"JFK", "LHR", "CDG", "JFK"};
        assertTrue(MultiCitySearch.hasFlightsForAllLegs(route, flightIndex));
    }

    @Test
    @DisplayName("hasFlightsForAllLegs returns false when a leg is missing from the index")
    void hasFlightsForAllLegsReturnsFalseWhenLegMissing() {
        HashMap<String, ArrayList<Flight>> partial = new HashMap<>();
        partial.put("JFKLHR", flightIndex.get("JFKLHR"));
        // LHRCDG and CDGJFK are absent

        String[] route = {"JFK", "LHR", "CDG", "JFK"};
        assertFalse(MultiCitySearch.hasFlightsForAllLegs(route, partial));
    }

    @Test
    @DisplayName("hasFlightsForAllLegs returns true for a single-leg route when the leg exists")
    void hasFlightsForAllLegsReturnsTrueForSingleLeg() {
        HashMap<String, ArrayList<Flight>> index = new HashMap<>();
        index.put("JFKLHR", flightIndex.get("JFKLHR"));

        assertTrue(MultiCitySearch.hasFlightsForAllLegs(new String[]{"JFK", "LHR"}, index));
    }

    @Test
    @DisplayName("hasFlightsForAllLegs returns false for a single-leg route when the leg is absent")
    void hasFlightsForAllLegsReturnsFalseForMissingSingleLeg() {
        assertFalse(MultiCitySearch.hasFlightsForAllLegs(new String[]{"JFK", "AMS"}, flightIndex));
    }

    @Test
    @DisplayName("computeLegDates assigns correct dates based on days at each airport")
    void computeLegDatesAssignsCorrectDates() {
        String[] perm = {"JFK", "LHR", "CDG", "JFK"};
        LocalDate[] dates = MultiCitySearch.computeLegDates(perm, LocalDate.of(2026, 4, 15), Map.of("LHR", 3, "CDG", 2));
        assertEquals(LocalDate.of(2026, 4, 15), dates[0]); // JFK→LHR
        assertEquals(LocalDate.of(2026, 4, 19), dates[1]); // LHR→CDG (15 + 3 + 1)
        assertEquals(LocalDate.of(2026, 4, 22), dates[2]); // CDG→JFK (19 + 2 + 1)
    }

    // -----------------------------------------------------------------------
    // searchByDateWithConnections tests
    //
    // Setup:
    //   Home: JFK. Destinations: [LHR, GYE] (Guayaquil).
    //   No direct LHR→GYE or GYE→LHR flights — must connect via UIO (Quito).
    //
    //   LHR→UIO: departs 10:00, distance 1363 km → arrives ~12:00 (2h flight)
    //   Valid same-day UIO→GYE: departs 15:00 (3h gap > MIN_CONNECTION_MINUTES) ✓
    //   Invalid same-day UIO→GYE: departs 13:00 (1h gap ≤ MIN_CONNECTION_MINUTES) ✗
    //   Overnight UIO→GYE: departs 08:00 (gap = -4h, treated as next calendar day) ✓
    // -----------------------------------------------------------------------

    private static final String FN_JFK_LHR = "AA100";
    private static final String FN_LHR_UIO = "LH1234";
    private static final String FN_UIO_GYE_VALID = "AV5000";
    private static final String FN_UIO_GYE_INVALID = "AV5001";
    private static final String FN_UIO_GYE_NEXTDAY = "AV5002";
    private static final String FN_GYE_JFK = "UA9999";

    // 1363 km gives exactly 2h flight time at 852 km/h + 0.4h overhead
    private static final double LHR_UIO_DISTANCE_KM = 1363.0;
    private static final double UIO_GYE_DISTANCE_KM = 400.0;

    private Airport uio;
    private Airport gye;
    private Flight uioGyeValid;
    private Flight uioGyeInvalid;
    private Flight uioGyeNextDay;
    private HashMap<String, ArrayList<Flight>> connectionFlightIndex;
    private FlightGraph connectionGraph;

    private void setUpConnectionData() {
        uio = new Airport("UIO", "Mariscal Sucre International", -0.1292, -78.3575, 9228, 2813, "Quito", "Ecuador");
        gye = new Airport("GYE", "Jose Joaquin de Olmedo International", -2.1574, -79.8836, 19, 6, "Guayaquil", "Ecuador");

        Flight lhrUio = new Flight(lhr, uio, LHR_UIO_DISTANCE_KM, LocalTime.of(10, 0), FN_LHR_UIO);
        // departs 15:00 — 3h after LHR→UIO arrives 12:00, satisfies > 2h
        uioGyeValid   = new Flight(uio, gye, UIO_GYE_DISTANCE_KM, LocalTime.of(15, 0), FN_UIO_GYE_VALID);
        // departs 13:00 — only 1h after LHR→UIO arrives 12:00, violates > 2h
        uioGyeInvalid = new Flight(uio, gye, UIO_GYE_DISTANCE_KM, LocalTime.of(13, 0), FN_UIO_GYE_INVALID);
        // departs 08:00 — before arrival 12:00, treated as overnight (next calendar day)
        uioGyeNextDay = new Flight(uio, gye, UIO_GYE_DISTANCE_KM, LocalTime.of(8, 0),  FN_UIO_GYE_NEXTDAY);

        Flight jfkLhr = new Flight(jfk, lhr, 5570.0, LocalTime.of(9, 0), FN_JFK_LHR);
        Flight gyeJfk = new Flight(gye, jfk, 4700.0, LocalTime.of(10, 0), FN_GYE_JFK);
        Flight lhrJfk = new Flight(lhr, jfk, 5570.0, LocalTime.of(11, 0), "AA101");
        Flight jfkGye = new Flight(jfk, gye, 4700.0, LocalTime.of(8, 0), "UA0001");
        Flight uioLhr = new Flight(uio, lhr, LHR_UIO_DISTANCE_KM, LocalTime.of(14, 0), "LH1235");
        Flight gyeUio = new Flight(gye, uio, UIO_GYE_DISTANCE_KM, LocalTime.of(6, 0), "AV5003");

        connectionFlightIndex = new HashMap<>();
        connectionFlightIndex.put("JFKLHR", new ArrayList<>(List.of(jfkLhr)));
        connectionFlightIndex.put("LHRJFK", new ArrayList<>(List.of(lhrJfk)));
        connectionFlightIndex.put("JFKGYE", new ArrayList<>(List.of(jfkGye)));
        connectionFlightIndex.put("GYEJFK", new ArrayList<>(List.of(gyeJfk)));
        connectionFlightIndex.put("LHRUIO", new ArrayList<>(List.of(lhrUio)));
        connectionFlightIndex.put("UIOLHR", new ArrayList<>(List.of(uioLhr)));
        connectionFlightIndex.put("UIOGYE", new ArrayList<>(List.of(uioGyeValid, uioGyeInvalid, uioGyeNextDay)));
        connectionFlightIndex.put("GYEUIO", new ArrayList<>(List.of(gyeUio)));
        // No LHRGYE or GYELHO — these require a connection via UIO

        connectionGraph = new FlightGraph(true, true);
        AirportVertex vJfk = connectionGraph.addVertex(jfk);
        AirportVertex vLhr = connectionGraph.addVertex(lhr);
        AirportVertex vUio = connectionGraph.addVertex(uio);
        AirportVertex vGye = connectionGraph.addVertex(gye);

        connectionGraph.addEdge(vJfk, vLhr, jfkLhr.getPrice(), jfkLhr.getDuration(), FN_JFK_LHR);
        connectionGraph.addEdge(vLhr, vJfk, lhrJfk.getPrice(), lhrJfk.getDuration(), "AA101");
        connectionGraph.addEdge(vJfk, vGye, jfkGye.getPrice(), jfkGye.getDuration(), "UA0001");
        connectionGraph.addEdge(vGye, vJfk, gyeJfk.getPrice(), gyeJfk.getDuration(), FN_GYE_JFK);
        connectionGraph.addEdge(vLhr, vUio, lhrUio.getPrice(), lhrUio.getDuration(), FN_LHR_UIO);
        connectionGraph.addEdge(vUio, vLhr, uioLhr.getPrice(), uioLhr.getDuration(), "LH1235");
        connectionGraph.addEdge(vUio, vGye, uioGyeValid.getPrice(), uioGyeValid.getDuration(), FN_UIO_GYE_VALID);
        connectionGraph.addEdge(vGye, vUio, gyeUio.getPrice(), gyeUio.getDuration(), "AV5003");
        // No direct LHR→GYE or GYE→LHR edge
    }

    // Returns a copy of connectionFlightIndex with UIOGYE replaced by the given options only.
    private HashMap<String, ArrayList<Flight>> connectionIndexWith(Flight... uioGyeOptions) {
        HashMap<String, ArrayList<Flight>> idx = new HashMap<>(connectionFlightIndex);
        idx.put("UIOGYE", new ArrayList<>(Arrays.asList(uioGyeOptions)));
        return idx;
    }

    @Test
    @DisplayName("searchByDateWithConnections finds a route via a connection airport when no direct flight exists")
    void connectionSearchFindsRouteViaConnection() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionFlightIndex);
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        assertFalse(routes.isEmpty(), "Expected at least one route via UIO connection");
        boolean hasUio = routes.stream()
                .anyMatch(r -> Arrays.asList(r.getAirports()).contains("UIO"));
        assertTrue(hasUio, "Expected at least one route to include UIO as a connection airport");
    }

    @Test
    @DisplayName("searchByDateWithConnections marks UIO legs as connection legs")
    void connectionSearchSetsIsConnectionLegFlag() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionFlightIndex);
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        Route routeViaUio = routes.stream()
                .filter(r -> Arrays.asList(r.getAirports()).contains("UIO"))
                .findFirst().orElse(null);
        assertNotNull(routeViaUio);

        String[] airports = routeViaUio.getAirports();
        int uioIndex = -1;
        for (int i = 0; i < airports.length; i++) {
            if ("UIO".equals(airports[i])) { uioIndex = i; break; }
        }
        // The leg ending at UIO (i.e. leg uioIndex - 1) should be a connection leg
        assertTrue(routeViaUio.isConnectionLeg(uioIndex - 1),
                "Leg ending at UIO should be flagged as a connection leg");
        assertFalse(routeViaUio.isConnectionLeg(uioIndex),
                "Leg departing UIO to an intended destination should not be a connection leg");
    }

    @Test
    @DisplayName("searchByDateWithConnections excludes connections where gap is <= 2h")
    void connectionSearchRejectsInsufficientConnectionTime() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionIndexWith(uioGyeInvalid));
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        // No route should contain the LHR→UIO→GYE sequence; the invalid gap rejects it
        boolean hasLhrUioGye = routes.stream().anyMatch(r -> {
            List<String> airports = Arrays.asList(r.getAirports());
            int lhrIdx = airports.indexOf("LHR");
            int uioIdx = airports.lastIndexOf("UIO");
            int gyeIdx = airports.indexOf("GYE");
            return lhrIdx >= 0 && uioIdx == lhrIdx + 1 && gyeIdx == uioIdx + 1;
        });
        assertFalse(hasLhrUioGye, "LHR→UIO→GYE should be excluded when only an insufficient connection gap exists");
    }

    @Test
    @DisplayName("searchByDateWithConnections accepts a same-day connection with > 2h gap")
    void connectionSearchAcceptsValidSameDayConnection() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionIndexWith(uioGyeValid));
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        boolean hasUio = routes.stream()
                .anyMatch(r -> Arrays.asList(r.getAirports()).contains("UIO"));
        assertTrue(hasUio, "Expected a route via UIO with a valid same-day connection");
    }

    @Test
    @DisplayName("searchByDateWithConnections accepts an overnight connection where outbound departs before inbound arrives")
    void connectionSearchAcceptsOvernightConnection() {
        // uioGyeNextDay departs 08:00; LHR→UIO arrives ~12:00 → gap = -4h → overnight valid
        MultiCitySearch mcs = new MultiCitySearch(null, connectionIndexWith(uioGyeNextDay));
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        boolean hasUio = routes.stream()
                .anyMatch(r -> Arrays.asList(r.getAirports()).contains("UIO"));
        assertTrue(hasUio, "Expected a route via UIO using the overnight connection");
    }

    @Test
    @DisplayName("searchByDateWithConnections returns no UIO routes when UIO-GYE and GYE-UIO flights are absent")
    void connectionSearchReturnsEmptyWhenNoConnectionFlightsExist() {
        HashMap<String, ArrayList<Flight>> noUioIndex = new HashMap<>(connectionFlightIndex);
        noUioIndex.remove("UIOGYE");
        noUioIndex.remove("GYEUIO");

        MultiCitySearch mcs = new MultiCitySearch(null, noUioIndex);
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("searchByDateWithConnections all routes start and end at the home airport")
    void connectionSearchAllRoutesStartAndEndAtHome() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionFlightIndex);
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        for (Route r : routes) {
            String[] airports = r.getAirports();
            assertEquals("JFK", airports[0]);
            assertEquals("JFK", airports[airports.length - 1]);
        }
    }

    @Test
    @DisplayName("searchByDateWithConnections stores intended airports without connection airports")
    void connectionSearchStoresIntendedAirports() {
        MultiCitySearch mcs = new MultiCitySearch(null, connectionFlightIndex);
        ArrayList<Route> routes = mcs.searchByDateWithConnections(
                "JFK", new String[]{"LHR", "GYE"}, "price", connectionGraph);

        Route routeViaUio = routes.stream()
                .filter(r -> Arrays.asList(r.getAirports()).contains("UIO"))
                .findFirst().orElse(null);
        assertNotNull(routeViaUio);

        // Intended airports must not include UIO
        List<String> intended = Arrays.asList(routeViaUio.getIntendedAirports());
        assertFalse(intended.contains("UIO"), "UIO should not appear in intended airports");
        assertTrue(intended.contains("LHR"));
        assertTrue(intended.contains("GYE"));
    }
}
