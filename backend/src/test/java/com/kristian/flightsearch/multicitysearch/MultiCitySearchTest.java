package com.kristian.flightsearch.multicitysearch;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    }

    @Test
    @DisplayName("search returns valid routes when flights exist for all legs")
    void searchReturnsRoutesWhenFlightsExist() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"});
        assertFalse(routes.isEmpty());
    }

    @Test
    @DisplayName("search returns empty list when no flights exist for a leg")
    void searchReturnsEmptyWhenLegMissing() {
        HashMap<String, ArrayList<Flight>> sparseIndex = new HashMap<>();
        sparseIndex.put("JFKLHR", flightIndex.get("JFKLHR"));
        // No return legs — no complete route is possible

        MultiCitySearch mcs = new MultiCitySearch(null, sparseIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR"});
        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("search returns routes sorted cheapest first")
    void searchReturnsCheapestRouteFirst() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"});
        assertTrue(routes.size() > 1);
        for (int i = 0; i < routes.size() - 1; i++) {
            assertTrue(routes.get(i).getCheapestTotalPrice() <= routes.get(i + 1).getCheapestTotalPrice());
        }
    }

    @Test
    @DisplayName("single destination produces a round-trip route")
    void singleDestinationProducesRoundTrip() {
        HashMap<String, ArrayList<Flight>> index = new HashMap<>();
        index.put("JFKLHR", flightIndex.get("JFKLHR"));
        index.put("LHRJFK", flightIndex.get("LHRJFK"));

        MultiCitySearch mcs = new MultiCitySearch(null, index);
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR"});

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
        ArrayList<Route> routes = mcs.search("JFK", new String[]{"LHR", "CDG"});

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

    // -----------------------------------------------------------------------
    // searchByDate tests
    //
    // Setup: departureDate = 2026-04-15, destinations = [LHR, CDG] from JFK
    //   daysAtAirport: LHR=3, CDG=2
    //
    // Permutation JFK→LHR→CDG→JFK:
    //   JFK→LHR on 2026-04-15
    //   LHR→CDG on 2026-04-19  (Apr15 + 3 days + 1)
    //   CDG→JFK on 2026-04-22  (Apr19 + 2 days + 1)
    //
    // Permutation JFK→CDG→LHR→JFK:
    //   JFK→CDG on 2026-04-15
    //   CDG→LHR on 2026-04-18  (Apr15 + 2 days + 1)
    //   LHR→JFK on 2026-04-22  (Apr18 + 3 days + 1)
    // -----------------------------------------------------------------------

    private static final LocalDate DEPARTURE = LocalDate.of(2026, 4, 15);

    private HashMap<String, ArrayList<Flight>> buildDateKeyedIndex() {
        HashMap<String, ArrayList<Flight>> idx = new HashMap<>();
        idx.put("JFKLHR2026-04-15", new ArrayList<>(List.of(jfkLhr())));
        idx.put("LHRCDG2026-04-19", new ArrayList<>(List.of(lhrCdg())));
        idx.put("CDGJFK2026-04-22", new ArrayList<>(List.of(cdgJfk())));
        idx.put("JFKCDG2026-04-15", new ArrayList<>(List.of(jfkCdg())));
        idx.put("CDGLHR2026-04-18", new ArrayList<>(List.of(cdgLhr())));
        idx.put("LHRJFK2026-04-22", new ArrayList<>(List.of(lhrJfk())));
        return idx;
    }

    private Flight jfkLhr() {
        Flight f = new Flight(jfk, lhr, 5570.0, LocalTime.of(9, 0), "AA100");
        f.setPrice(300);
        return f;
    }
    private Flight lhrCdg() {
        Flight f = new Flight(lhr, cdg, 344.0, LocalTime.of(14, 0), "BA302");
        f.setPrice(100);
        return f;
    }
    private Flight cdgJfk() {
        Flight f = new Flight(cdg, jfk, 5837.0, LocalTime.of(16, 0), "AF006");
        f.setPrice(350);
        return f;
    }
    private Flight lhrJfk() {
        Flight f = new Flight(lhr, jfk, 5570.0, LocalTime.of(11, 0), "AA101");
        f.setPrice(320);
        return f;
    }
    private Flight jfkCdg() {
        Flight f = new Flight(jfk, cdg, 5837.0, LocalTime.of(8, 0), "AF007");
        f.setPrice(280);
        return f;
    }
    private Flight cdgLhr() {
        Flight f = new Flight(cdg, lhr, 344.0, LocalTime.of(12, 0), "BA303");
        f.setPrice(90);
        return f;
    }

    @Test
    @DisplayName("searchByDate returns valid routes when all legs have flights on correct dates")
    void searchByDateReturnsRoutesWhenAllLegsPresent() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        ArrayList<Route> routes = mcs.searchByDateWithIndex(
                "JFK", new String[]{"LHR", "CDG"}, DEPARTURE, days, "price", buildDateKeyedIndex());

        assertFalse(routes.isEmpty());
    }

    @Test
    @DisplayName("searchByDate excludes permutations where a leg has no flights on its required date")
    void searchByDateExcludesPermutationWithMissingLegOnDate() {
        // Only stock JFK→LHR→CDG→JFK dates; CDG→LHR and LHR→JFK are absent
        HashMap<String, ArrayList<Flight>> partialIndex = new HashMap<>();
        partialIndex.put("JFKLHR2026-04-15", new ArrayList<>(List.of(jfkLhr())));
        partialIndex.put("LHRCDG2026-04-19", new ArrayList<>(List.of(lhrCdg())));
        partialIndex.put("CDGJFK2026-04-22", new ArrayList<>(List.of(cdgJfk())));

        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        ArrayList<Route> routes = mcs.searchByDateWithIndex(
                "JFK", new String[]{"LHR", "CDG"}, DEPARTURE, days, "price", partialIndex);

        assertEquals(1, routes.size());
        assertArrayEquals(new String[]{"JFK", "LHR", "CDG", "JFK"}, routes.get(0).getAirports());
    }

    @Test
    @DisplayName("searchByDate returns empty list when no legs have flights on their required dates")
    void searchByDateReturnsEmptyWhenNoFlightsOnDates() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        ArrayList<Route> routes = mcs.searchByDateWithIndex(
                "JFK", new String[]{"LHR", "CDG"}, DEPARTURE, days, "price", new HashMap<>());

        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("searchByDate sorts by cheapest total price when optimizeBy is price")
    void searchByDateSortsByPriceWhenOptimizeByPrice() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        ArrayList<Route> routes = mcs.searchByDateWithIndex(
                "JFK", new String[]{"LHR", "CDG"}, DEPARTURE, days, "price", buildDateKeyedIndex());

        assertTrue(routes.size() > 1);
        for (int i = 0; i < routes.size() - 1; i++) {
            assertTrue(routes.get(i).getCheapestTotalPrice() <= routes.get(i + 1).getCheapestTotalPrice());
        }
    }

    @Test
    @DisplayName("searchByDate sorts by shortest total duration when optimizeBy is duration")
    void searchByDateSortsByDurationWhenOptimizeByDuration() {
        MultiCitySearch mcs = new MultiCitySearch(null, flightIndex);
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        ArrayList<Route> routes = mcs.searchByDateWithIndex(
                "JFK", new String[]{"LHR", "CDG"}, DEPARTURE, days, "duration", buildDateKeyedIndex());

        assertTrue(routes.size() > 1);
        for (int i = 0; i < routes.size() - 1; i++) {
            assertTrue(routes.get(i).getShortestTotalDurationMinutes()
                    <= routes.get(i + 1).getShortestTotalDurationMinutes());
        }
    }

    @Test
    @DisplayName("computeLegDates assigns correct dates based on days at each airport")
    void computeLegDatesAssignsCorrectDates() {
        // JFK→LHR→CDG→JFK, 3 days at LHR, 2 days at CDG
        String[] perm = {"JFK", "LHR", "CDG", "JFK"};
        Map<String, Integer> days = Map.of("LHR", 3, "CDG", 2);

        LocalDate[] dates = MultiCitySearch.computeLegDates(perm, DEPARTURE, days);

        assertEquals(LocalDate.of(2026, 4, 15), dates[0]); // JFK→LHR
        assertEquals(LocalDate.of(2026, 4, 19), dates[1]); // LHR→CDG (15 + 3 + 1)
        assertEquals(LocalDate.of(2026, 4, 22), dates[2]); // CDG→JFK (19 + 2 + 1)
    }
}
