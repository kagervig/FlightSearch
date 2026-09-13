package com.kristian.flightsearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.kristian.flightsearch.models.FlightResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalTime;
import java.util.ArrayList;

@DisplayName("RouteBuilder tests")
class RouteBuilderTest {

    private ByteArrayOutputStream out;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("printFlightResults() prints a header with column names")
    void testPrintFlightResultsHeader() {
        RouteBuilder.printFlightResults(new ArrayList<>());
        String output = out.toString();
        assertTrue(output.contains("Price"));
        assertTrue(output.contains("Destination"));
        assertTrue(output.contains("Flight"));
        assertTrue(output.contains("Airline"));
    }

    @Test
    @DisplayName("printFlightResults() always prints the Return Home row as option 0")
    void testPrintFlightResultsReturnHomeRow() {
        RouteBuilder.printFlightResults(new ArrayList<>());
        assertTrue(out.toString().contains("Return Home"));
    }

    @Test
    @DisplayName("printFlightResults() does not throw on an empty list")
    void testPrintFlightResultsEmptyList() {
        assertDoesNotThrow(() -> RouteBuilder.printFlightResults(new ArrayList<>()));
    }

    @Test
    @DisplayName("printFlightResults() includes destination and price for each flight")
    void testPrintFlightResultsContent() {
        ArrayList<FlightResult> results = new ArrayList<>();
        results.add(new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", LocalTime.of(10, 30)));
        RouteBuilder.printFlightResults(results);
        String output = out.toString();
        assertTrue(output.contains("LAX"));
        assertTrue(output.contains("299"));
        assertTrue(output.contains("AA 1234"));
        assertTrue(output.contains("AA"));
    }

    @Test
    @DisplayName("printFlightResults() numbers rows starting from 1")
    void testPrintFlightResultsNumbering() {
        ArrayList<FlightResult> results = new ArrayList<>();
        results.add(new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", LocalTime.of(10, 30)));
        results.add(new FlightResult("UA 5678", "JFK", "ORD", 199, "UA", LocalTime.of(14, 0)));
        RouteBuilder.printFlightResults(results);
        String output = out.toString();
        assertTrue(output.contains("LAX"));
        assertTrue(output.contains("ORD"));
    }

    @Test
    @DisplayName("printFlightResults() prints all flights in the list")
    void testPrintFlightResultsPrintsAll() {
        ArrayList<FlightResult> results = new ArrayList<>();
        results.add(new FlightResult("AA 1234", "JFK", "LAX", 299, "AA", LocalTime.of(10, 30)));
        results.add(new FlightResult("UA 5678", "JFK", "ORD", 199, "UA", LocalTime.of(14, 0)));
        results.add(new FlightResult("DL 9012", "JFK", "MIA", 149, "DL", LocalTime.of(8, 0)));
        RouteBuilder.printFlightResults(results);
        String output = out.toString();
        assertTrue(output.contains("LAX"));
        assertTrue(output.contains("ORD"));
        assertTrue(output.contains("MIA"));
    }
}
