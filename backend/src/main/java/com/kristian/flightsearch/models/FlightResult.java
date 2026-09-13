package com.kristian.flightsearch.models;

import java.time.LocalTime;

/** Lightweight query result for a single flight leg — used by route search before full Flight objects are needed. */
public record FlightResult(String flightNumber, String origin, String destination, int price, String airline, LocalTime departureTime) {}
