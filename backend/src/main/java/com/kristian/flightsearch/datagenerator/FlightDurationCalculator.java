package com.kristian.flightsearch.datagenerator;
import java.time.Duration;


public class FlightDurationCalculator {

    private static final double AVERAGE_CRUISE_SPEED_KNOTS = 460.0;

    /**
     * Calculate flight duration from distance in nautical miles.
     *
     * @param distanceNM Distance in nautical miles
     * @return LocalTime representing duration (hours, minutes)
     */
    public static Duration calculateFlightDuration(double distanceNM) {
        double avgSpeedKnots = AVERAGE_CRUISE_SPEED_KNOTS;
        
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