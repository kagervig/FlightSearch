package com.kristian.flightsearch.flightgraph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.kristian.flightsearch.models.Airport;

import java.time.Duration;
import java.util.Map;

/*
 * Graph used in all tests:
 *
 *   JFK --$100/2h--> ATL --$150/3h--> LAX
 *   JFK ----------$400/6h-----------> LAX
 *
 * Cheapest JFK -> LAX: $250 via ATL
 * Shortest JFK -> LAX: 5h via ATL (2h + 3h < 6h)
 */
@DisplayName("Dijkstra Tests")
class DijkstraTest {

    private FlightGraph graph;
    private AirportVertex jfkVertex;
    private AirportVertex atlVertex;
    private AirportVertex laxVertex;

    @BeforeEach
    void setUp() {
        Airport jfk = new Airport("JFK", "John F. Kennedy International Airport", 40.6398, -73.7789, 4423, 13, "New York", "United States");
        Airport atl = new Airport("ATL", "Hartsfield-Jackson Atlanta International Airport", 33.6404, -84.4199, 3962, 313, "Atlanta", "United States");
        Airport lax = new Airport("LAX", "Los Angeles International Airport", 33.9428, -118.4100, 3939, 38, "Los Angeles", "United States");

        graph = new FlightGraph(true, true);
        jfkVertex = graph.addVertex(jfk);
        atlVertex = graph.addVertex(atl);
        laxVertex = graph.addVertex(lax);

        graph.addEdge(jfkVertex, atlVertex, 100, Duration.ofHours(2), "AA 001");
        graph.addEdge(atlVertex, laxVertex, 150, Duration.ofHours(3), "AA 002");
        graph.addEdge(jfkVertex, laxVertex, 400, Duration.ofHours(6), "AA 003");
    }

    @Test
    @DisplayName("searchByPrice() finds cheapest route via intermediate stop")
    void testSearchByPriceCheapestViaStop() {
        @SuppressWarnings("unchecked")
        Map<Airport, Integer> prices = Dijkstra.searchByPrice(graph, jfkVertex)[0];

        assertEquals(100, prices.get(atlVertex.getData()));
        assertEquals(250, prices.get(laxVertex.getData()));
    }

    @Test
    @DisplayName("searchByPrice() sets origin distance to zero")
    void testSearchByPriceOriginIsZero() {
        @SuppressWarnings("unchecked")
        Map<Airport, Integer> prices = Dijkstra.searchByPrice(graph, jfkVertex)[0];

        assertEquals(0, prices.get(jfkVertex.getData()));
    }

    @Test
    @DisplayName("searchByDuration() finds shortest duration via intermediate stop")
    void testSearchByDurationShortestViaStop() {
        @SuppressWarnings("unchecked")
        Map<Airport, Duration> durations = Dijkstra.searchByDuration(graph, jfkVertex)[0];

        assertEquals(Duration.ofHours(2), durations.get(atlVertex.getData()));
        assertEquals(Duration.ofHours(5), durations.get(laxVertex.getData()));
    }

    @Test
    @DisplayName("searchByDuration() sets origin duration to zero")
    void testSearchByDurationOriginIsZero() {
        @SuppressWarnings("unchecked")
        Map<Airport, Duration> durations = Dijkstra.searchByDuration(graph, jfkVertex)[0];

        assertEquals(Duration.ZERO, durations.get(jfkVertex.getData()));
    }
}
