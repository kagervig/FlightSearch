package com.kristian.flightsearch.models;

/*
 * Represents a flight leg (origin, destination) used as a DB query parameter.
 */
public record LegQuery(String origin, String destination) {}
