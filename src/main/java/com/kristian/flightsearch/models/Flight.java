package com.kristian.flightsearch.models;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Random;

import com.kristian.flightsearch.datagenerator.FlightDurationCalculator;


public class Flight {
    Airport origin;
    Airport destination;
    double distance;
    LocalTime departureTime, arrivalTime;
    String flightNumber; 
    Duration duration;
    Integer price;
    private static final String[] AIRLINE_CODES = {"AA","UA","DL","WN","B6","AS","NK","F9","G4","SY"};

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
    public int flightPricer(int distance){
        Random random = new Random();
        
        // Base price: distance / 10
        Integer basePrice = (Integer) distance / 10;
        
        // Add random modifier: 50-250
        Integer randomModifier = random.nextInt(201) + 50; // 50-250
        Integer price = basePrice + randomModifier;
        
        // Apply chance-based multipliers
        Integer roll = random.nextInt(80); // 0-79, 1/80 chance
        if (roll == 0) {
            price *= 3; // Triple the price
        }
        
        roll = random.nextInt(50); // 0-49, 1/50 chance
        if (roll == 0) {
            price *= 2; // Double the price
        }
        
        roll = random.nextInt(6); // 0-5, 1/6 chance
        if (roll == 0) {
            price = (int)(price * 1.5); // Increase by 50%
        }
        
        roll = random.nextInt(4); // 0-3, 1/4 chance
        if (roll == 0) {
            price = (int)(price * 1.25); // Increase by 25%
        }
        
        return price;
    }
}

