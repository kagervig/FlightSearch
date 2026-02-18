package com.kristian.flightsearch.models;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Random;
import java.util.Set;

import com.kristian.flightsearch.datagenerator.FlightDurationCalculator;


public class Flight {
    Airport origin;
    Airport destination;
    double distance;
    LocalTime departureTime, arrivalTime;
    String flightNumber;
    Duration duration;
    Integer price;
    private static final String[] AIRLINE_CODES = {
        // North America
        "AA", "UA", "DL", "WN", "B6", "AS", "NK", "F9", "G4", "SY",
        "AC", "WS", "TS", "PD", "AM", "Y4", "4O", "HA", "QX", "OH",
        // Europe
        "BA", "LH", "AF", "KL", "IB", "AZ", "SK", "AY", "TP", "SN",
        "LX", "OS", "LO", "OK", "RO", "BT", "OU", "JU", "FR", "U2",
        "W6", "VY", "DS", "DY", "D8", "HV", "BE", "FI",
        // Middle East & Africa
        "EK", "QR", "EY", "SV", "GF", "WY", "RJ", "MS", "ET", "KQ",
        "SA", "AT", "TU",
        // Asia-Pacific
        "CX", "SQ", "QF", "NZ", "JL", "NH", "OZ", "KE", "TG", "MH",
        "GA", "PR", "VN", "CI", "BR", "CA", "CZ", "MU", "HU", "3U",
        "AI", "UK", "SL", "AK", "FD", "QZ", "JQ", "TT", "MM", "7C",
        // South America
        "LA", "JJ", "AR", "CM", "AV", "G3", "AD"
    };
    private static final Set<String> BUDGET_CARRIERS = Set.of(
        "NK", "F9", "G4", "SY", "WN", "Y4", "4O",       // North America
        "FR", "U2", "W6", "VY", "DS", "DY", "D8", "HV", // Europe
        "AK", "FD", "QZ", "JQ", "TT", "MM", "7C", "SL", // Asia-Pacific
        "G3", "AD"                                         // South America
    );

    public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime){
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.departureTime = departureTime;
        this.duration = FlightDurationCalculator.calculateFlightDuration(distance);
        this.arrivalTime = this.departureTime.plus(this.duration);
        this.flightNumber = generateFlightNum();
        this.price = flightPricer((int) distance);

    }
    public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime, String flightNumber){
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.departureTime = departureTime;
        this.duration = FlightDurationCalculator.calculateFlightDuration(distance);
        this.arrivalTime = this.departureTime.plus(this.duration);
        this.flightNumber = flightNumber;
        this.price = flightPricer((int) distance);
    }

    // Getters
    public Airport getOrigin() {
        return origin;
    }
    public Integer getPrice(){
        return price;
    }

    public Airport getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }
    public String getFlightNumber() {
        return flightNumber;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }
    public Duration getDuration() {
        return duration;
    }

    // Setters
    public void setOrigin(Airport origin) {
        this.origin = origin;
    }

    public void setDestination(Airport destination) {
        this.destination = destination;
    }
    public void setPrice(int price){
        this.price = price;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }
    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    public static String generateFlightNum(){
        Random random = new Random();
        int flightNum = random.nextInt(9999) + 1;
        int randomAirline = random.nextInt(AIRLINE_CODES.length);
        String airlineCode = AIRLINE_CODES[randomAirline];
        return airlineCode + String.format(" %04d", flightNum);
    }
    double getTimeOfDayFactor() {
        int hour = this.departureTime.getHour();
        if (hour < 6) return 0.80;       // red-eye
        if (hour < 9) return 0.90;       // early morning
        if (hour < 12) return 1.00;      // mid-morning
        if (hour < 17) return 1.05;      // afternoon
        if (hour < 21) return 1.15;      // evening peak
        return 0.85;                      // late night
    }

    double getAirlineTypeFactor() {
        String airlineCode = this.flightNumber.substring(0, 2);
        return BUDGET_CARRIERS.contains(airlineCode) ? 0.75 : 1.00;
    }

    static double getDemandFactor(Random random) {
        int roll = random.nextInt(100);
        if (roll < 5) return 0.70;       // deep discount / sale
        if (roll < 15) return 0.85;      // moderate discount
        if (roll < 65) return 1.00;      // standard fare
        if (roll < 85) return 1.20;      // busy period
        if (roll < 95) return 1.50;      // high demand
        return 2.00;                      // peak / last-minute premium
    }

    public int flightPricer(int distance) {
        Random random = new Random();

        double fixedCost = 75.0;
        double costPerKm = Math.max(0.06, 0.10 - 0.04 * (distance / 18000.0));
        double baseCost = fixedCost + costPerKm * distance;

        double timeOfDayFactor = getTimeOfDayFactor();
        double airlineTypeFactor = getAirlineTypeFactor();
        double demandFactor = getDemandFactor(random);

        int price = (int) (baseCost * timeOfDayFactor * airlineTypeFactor * demandFactor);
        return Math.max(price, 50);
    }
}

