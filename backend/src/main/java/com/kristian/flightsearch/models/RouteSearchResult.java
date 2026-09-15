package com.kristian.flightsearch.models;

import java.time.LocalTime;

/** Enriched flight leg for the route-builder API — includes destination city, distance, and duration. */
public record RouteSearchResult(
    String flightNumber,
    String origin,
    String destination,
    String destinationCity,
    int price,
    String airline,
    LocalTime departureTime,
    int distanceKm,
    int durationMinutes
) {}
