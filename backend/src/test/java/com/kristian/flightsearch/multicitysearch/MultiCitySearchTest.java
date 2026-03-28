package com.kristian.flightsearch.multicitysearch;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
}
