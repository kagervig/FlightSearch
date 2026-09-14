package com.kristian.flightsearch.models;

import java.util.List;

/** Response for the fly-home endpoint: one direct leg or a sequence of Dijkstra-derived legs. */
public record FlyHomeResult(boolean direct, List<RouteSearchResult> legs, int totalPrice) {}
