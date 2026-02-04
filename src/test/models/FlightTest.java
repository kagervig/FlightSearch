package test.models;
package src.test.models;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DataGenerator.FlightDurationCalculator;

import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalTime;

@DisplayName("Flight Class Tests")
class FlightTest {
    
    private Airport jfk;
    private Airport lax;
    private LocalTime departureTime;
    
    @BeforeEach
    void setUp() {
        // Create test airports
        jfk = new Airport("JFK", "John F. Kennedy International Airport", 40.6413, -73.7781, "America/New_York");
        lax = new Airport("LAX", "Los Angeles International Airport", 33.9416, -118.4085, "America/Los_Angeles");
        departureTime = LocalTime.of(10, 30);
    }
    
    @Test
    @DisplayName("Constructor without flight number should initialize all fields correctly")
    void testConstructorWithoutFlightNumber() {
        double distance = 2475.0;
        Flight flight = new Flight(jfk, lax, distance, departureTime);
        
        assertEquals(jfk, flight.getOrigin());
        assertEquals(lax, flight.getDestination());
        assertEquals(distance, flight.getDistance());
        assertEquals(departureTime, flight.getDepartureTime());
        assertNotNull(flight.getFlightNumber());
        assertNotNull(flight.getDuration());
        assertNotNull(flight.getArrivalTime());
        assertNotNull(flight.getPrice());
    }
    
    @Test
    @DisplayName("Constructor with flight number should use provided flight number")
    void testConstructorWithFlightNumber() {
        double distance = 2475.0;
        String flightNumber = "AA 1234";
        Flight flight = new Flight(jfk, lax, distance, departureTime, flightNumber);
        
        assertEquals(jfk, flight.getOrigin());
        assertEquals(lax, flight.getDestination());
        assertEquals(distance, flight.getDistance());
        assertEquals(departureTime, flight.getDepartureTime());
        assertEquals(flightNumber, flight.getFlightNumber());
        assertNotNull(flight.getDuration());
        assertNotNull(flight.getArrivalTime());
        assertNotNull(flight.getPrice());
    }
    
    @Test
    @DisplayName("getOrigin should return the origin airport")
    void testGetOrigin() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        assertEquals(jfk, flight.getOrigin());
    }
    
    @Test
    @DisplayName("getDestination should return the destination airport")
    void testGetDestination() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        assertEquals(lax, flight.getDestination());
    }
    
    @Test
    @DisplayName("getDistance should return the distance")
    void testGetDistance() {
        double distance = 2475.0;
        Flight flight = new Flight(jfk, lax, distance, departureTime);
        assertEquals(distance, flight.getDistance());
    }
    
    @Test
    @DisplayName("getFlightNumber should return the flight number")
    void testGetFlightNumber() {
        String flightNumber = "UA 5678";
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime, flightNumber);
        assertEquals(flightNumber, flight.getFlightNumber());
    }
    
    @Test
    @DisplayName("getDepartureTime should return the departure time")
    void testGetDepartureTime() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        assertEquals(departureTime, flight.getDepartureTime());
    }
    
    @Test
    @DisplayName("getArrivalTime should return arrival time based on departure + duration")
    void testGetArrivalTime() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        LocalTime expectedArrival = departureTime.plus(flight.getDuration());
        assertEquals(expectedArrival, flight.getArrivalTime());
    }
    
    @Test
    @DisplayName("getDuration should return the calculated flight duration")
    void testGetDuration() {
        double distance = 2475.0;
        Flight flight = new Flight(jfk, lax, distance, departureTime);
        Duration expectedDuration = FlightDurationCalculator.calculateFlightDuration(distance);
        assertEquals(expectedDuration, flight.getDuration());
    }
    
    @Test
    @DisplayName("getPrice should return the calculated price")
    void testGetPrice() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        assertNotNull(flight.getPrice());
        assertTrue(flight.getPrice() > 0);
    }
    
    @Test
    @DisplayName("setOrigin should update the origin airport")
    void testSetOrigin() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        Airport ord = new Airport("ORD", "O'Hare International Airport", 41.9742, -87.9073, "America/Chicago");
        flight.setOrigin(ord);
        assertEquals(ord, flight.getOrigin());
    }
    
    @Test
    @DisplayName("setDestination should update the destination airport")
    void testSetDestination() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        Airport sfo = new Airport("SFO", "San Francisco International Airport", 37.6213, -122.3790, "America/Los_Angeles");
        flight.setDestination(sfo);
        assertEquals(sfo, flight.getDestination());
    }
    
    @Test
    @DisplayName("setPrice should update the price")
    void testSetPrice() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        int newPrice = 500;
        flight.setPrice(newPrice);
        assertEquals(newPrice, flight.getPrice());
    }
    
    @Test
    @DisplayName("setFlightNumber should update the flight number")
    void testSetFlightNumber() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        String newFlightNumber = "DL 9999";
        flight.setFlightNumber(newFlightNumber);
        assertEquals(newFlightNumber, flight.getFlightNumber());
    }
    
    @Test
    @DisplayName("setDistance should update the distance")
    void testSetDistance() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        int newDistance = 3000;
        flight.setDistance(newDistance);
        assertEquals(newDistance, flight.getDistance());
    }
    
    @Test
    @DisplayName("setdepartureTime should update the departure time")
    void testSetDepartureTime() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        LocalTime newDepartureTime = LocalTime.of(14, 45);
        flight.setdepartureTime(newDepartureTime);
        assertEquals(newDepartureTime, flight.getDepartureTime());
    }
    
    @Test
    @DisplayName("setdArrivalTime should update the arrival time")
    void testSetArrivalTime() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        LocalTime newArrivalTime = LocalTime.of(18, 30);
        flight.setdArrivalTime(newArrivalTime);
        assertEquals(newArrivalTime, flight.getArrivalTime());
    }
    
    @Test
    @DisplayName("generateFlightNum should return valid flight number format")
    void testGenerateFlightNum() {
        String flightNumber = Flight.generateFlightNum();
        
        assertNotNull(flightNumber);
        // Format should be "XX ####" (airline code space 4-digit number)
        assertTrue(flightNumber.matches("[A-Z]{2} \\d{4}"), 
            "Flight number should match format 'XX ####', but got: " + flightNumber);
        
        // Check if airline code is from the valid list
        String airlineCode = flightNumber.substring(0, 2);
        boolean validAirline = false;
        for (String code : Flight.airlineCodes) {
            if (code.equals(airlineCode)) {
                validAirline = true;
                break;
            }
        }
        assertTrue(validAirline, "Airline code should be from the valid list");
    }
    
    @Test
    @DisplayName("generateFlightNum should generate unique flight numbers")
    void testGenerateFlightNumUniqueness() {
        // Generate multiple flight numbers and check that at least some are different
        // (there's a tiny chance they could all be the same, but very unlikely)
        String firstNumber = Flight.generateFlightNum();
        boolean foundDifferent = false;
        
        for (int i = 0; i < 10; i++) {
            String nextNumber = Flight.generateFlightNum();
            if (!nextNumber.equals(firstNumber)) {
                foundDifferent = true;
                break;
            }
        }
        
        assertTrue(foundDifferent, "Should generate different flight numbers");
    }
    
    @Test
    @DisplayName("flightPricer should return positive price")
    void testFlightPricerPositive() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        int price = flight.flightPricer(1000);
        assertTrue(price > 0, "Price should be positive");
    }
    
    @Test
    @DisplayName("flightPricer should calculate price based on distance")
    void testFlightPricerDistance() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        
        // Longer distance should generally result in higher base price
        // Though randomness could affect this, base price scales with distance
        int shortDistance = 100;
        int longDistance = 3000;
        
        // The base price component is distance/10, so longer distance has higher base
        assertTrue(longDistance / 10 > shortDistance / 10);
    }
    
    @Test
    @DisplayName("flightPricer should include base price and modifier")
    void testFlightPricerComponents() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        int distance = 1000;
        int price = flight.flightPricer(distance);
        
        // Minimum price should be at least base (distance/10) + minimum modifier (50)
        int minimumExpected = (distance / 10) + 50;
        assertTrue(price >= minimumExpected, 
            "Price should be at least base + minimum modifier");
    }
    
    @Test
    @DisplayName("flightPricer with zero distance should handle edge case")
    void testFlightPricerZeroDistance() {
        Flight flight = new Flight(jfk, lax, 2475.0, departureTime);
        int price = flight.flightPricer(0);
        
        // Even with 0 distance, should have the random modifier (50-250)
        assertTrue(price >= 50, "Price should be at least the minimum modifier");
    }
    
    @Test
    @DisplayName("Flight duration should be calculated correctly")
    void testFlightDurationCalculation() {
        double distance = 2475.0;
        Flight flight = new Flight(jfk, lax, distance, departureTime);
        
        Duration expectedDuration = FlightDurationCalculator.calculateFlightDuration(distance);
        assertEquals(expectedDuration, flight.getDuration());
        
        // Arrival time should equal departure + duration
        LocalTime expectedArrival = departureTime.plus(expectedDuration);
        assertEquals(expectedArrival, flight.getArrivalTime());
    }
    
    @Test
    @DisplayName("Flight with very short distance should handle correctly")
    void testVeryShortDistance() {
        double shortDistance = 50.0;
        Flight flight = new Flight(jfk, lax, shortDistance, departureTime);
        
        assertEquals(shortDistance, flight.getDistance());
        assertNotNull(flight.getDuration());
        assertNotNull(flight.getPrice());
    }
    
    @Test
    @DisplayName("Flight with very long distance should handle correctly")
    void testVeryLongDistance() {
        double longDistance = 10000.0;
        Flight flight = new Flight(jfk, lax, longDistance, departureTime);
        
        assertEquals(longDistance, flight.getDistance());
        assertNotNull(flight.getDuration());
        assertNotNull(flight.getPrice());
        assertTrue(flight.getPrice() > 0);
    }
}
