package com.kristian.flightsearch.datagenerator;
import java.time.Duration;


public class FlightDurationCalculator {
    
    /**
     * Calculate flight duration from distance in nautical miles.
     * 
     * @param distanceNM Distance in nautical miles
     * @return LocalTime representing duration (hours, minutes)
     */
    public static Duration calculateFlightDuration(double distanceNM) {
        // Average speed in knots (nautical miles per hour)
        // Commercial jets cruise at ~450-480 knots
        double avgSpeedKnots = 460.0;
        
        // Base flight time in hours
        double baseTimeHours = distanceNM / avgSpeedKnots;
        
        // Add overhead for taxi, takeoff, landing (in hours)
        double overhead;
        if (distanceNM < 250) {
            overhead = 0.5;  // 30 minutes for short flights
        } else if (distanceNM < 1000) {
            overhead = 0.4;  // 24 minutes for medium flights
        } else {
            overhead = 0.35; // 21 minutes for long flights
        }
        
        // Total time in hours
        double totalHours = baseTimeHours + overhead;
        
        // Convert to hours and minutes
        int hours = (int) totalHours;
        int minutes = (int) Math.round((totalHours - hours) * 60);
        
        // Handle rounding edge case (60 minutes = 1 hour)
        if (minutes == 60) {
            hours++;
            minutes = 0;
        }
        
        Duration flightDuration = Duration.ofHours(hours).plusMinutes(minutes);

        return flightDuration;
    }
    
    // Example usage
    public static void main(String[] args) {

    }
}