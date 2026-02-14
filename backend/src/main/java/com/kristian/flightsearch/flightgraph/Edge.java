package com.kristian.flightsearch.flightgraph;

import java.time.Duration;

public class Edge {
    private AirportVertex start;
    private AirportVertex end;
    private Integer price;
    private Duration duration;
    private String flightNumber;

    public Edge(AirportVertex start, AirportVertex end, Integer price, Duration duration, String flightNumber) {
        this.start = start;
        this.end = end;
        this.price = price;
        this.duration = duration;
        this.flightNumber = flightNumber;

    }

    public AirportVertex getStart() {
        return this.start;
    }

    public AirportVertex getEnd() {
        return this.end;
    }

    public Integer getPrice() {
        return this.price;
    }
    public Duration getDuration(){
        return this.duration;
    }

    public String getFlightNum() {
        return this.flightNumber;
    }

}
