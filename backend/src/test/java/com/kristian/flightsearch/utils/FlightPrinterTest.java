package com.kristian.flightsearch.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalTime;

@DisplayName("FlightPrinter tests")
class FlightPrinterTest {

    private FlightPrinter printer;
    private ByteArrayOutputStream out;
    private PrintStream originalOut;
    private Airport jfk;
    private Airport lax;

    @BeforeEach
    void setUp() {
        printer = new FlightPrinter();
        out = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(out));

        jfk = new Airport("JFK", "John F. Kennedy International Airport", 40.6413, -73.7781, 4423, 13, "New York", "United States");
        lax = new Airport("LAX", "Los Angeles International Airport", 33.9416, -118.4085, 3939, 38, "Los Angeles", "United States");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("print(null) writes an error message")
    void testPrintNull() {
        printer.print(null);
        assertTrue(out.toString().contains("Error"));
    }

    @Test
    @DisplayName("print() includes the flight number")
    void testPrintFlightNumber() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        printer.print(flight);
        assertTrue(out.toString().contains("AA 1234"));
    }

    @Test
    @DisplayName("print() includes origin and destination IATA codes")
    void testPrintAirportCodes() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        printer.print(flight);
        String output = out.toString();
        assertTrue(output.contains("JFK"));
        assertTrue(output.contains("LAX"));
    }

    @Test
    @DisplayName("print() includes origin and destination city names")
    void testPrintCityNames() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        printer.print(flight);
        String output = out.toString();
        assertTrue(output.contains("New York"));
        assertTrue(output.contains("Los Angeles"));
    }

    @Test
    @DisplayName("print() includes departure time formatted as HH:mm")
    void testPrintDepartureTime() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        printer.print(flight);
        assertTrue(out.toString().contains("10:30"));
    }

    @Test
    @DisplayName("print() includes the ticket price")
    void testPrintPrice() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        flight.setPrice(350);
        printer.print(flight);
        assertTrue(out.toString().contains("350"));
    }

    @Test
    @DisplayName("print() includes a duration string")
    void testPrintDuration() {
        Flight flight = new Flight(jfk, lax, 2475.0, LocalTime.of(10, 30), "AA 1234");
        printer.print(flight);
        // Duration format is "Xh YYm"
        assertTrue(out.toString().matches("(?s).*\\d+h \\d{2}m.*"));
    }
}
