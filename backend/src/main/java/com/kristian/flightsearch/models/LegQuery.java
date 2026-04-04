package com.kristian.flightsearch.models;

import java.time.LocalDate;

/*
 * Represents a specific flight leg (origin, destination, date) needed for a DB query.
 */
public record LegQuery(String origin, String destination, LocalDate date) {}
