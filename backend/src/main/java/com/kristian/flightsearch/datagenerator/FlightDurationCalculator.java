package com.kristian.flightsearch.datagenerator;
import java.time.Duration;


public class FlightDurationCalculator {

    private static final double AVERAGE_CRUISE_SPEED_KMH = 852.0;

    /**
     * Calculate flight duration from distance in kilometres.
     *
     * @param distanceKm Distance in kilometres
     * @return Duration representing flight time (hours, minutes)
     */
    public static Duration calculateFlightDuration(double distanceKm) {
        // Base flight time in hours
        double baseTimeHours = distanceKm / AVERAGE_CRUISE_SPEED_KMH;

        // Add overhead for taxi, takeoff, landing (in hours)
        double overhead;
        if (distanceKm < 463) {           // ~250 NM
            overhead = 0.5;  // 30 minutes for short flights
        } else if (distanceKm < 1852) {   // ~1000 NM
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