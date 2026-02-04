package com.kristian.flightsearch.flightgraph;
public class Edge {
    private AirportVertex start;
    private AirportVertex end;
    private Integer weight;
    private String flightNumber;

    public Edge(AirportVertex start, AirportVertex end, Integer weight, String flightNumber){
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.flightNumber = flightNumber;

    }

    public AirportVertex getStart(){
        return this.start;
    }
    public AirportVertex getEnd(){
        return this.end;
    }
    public Integer getWeight(){
        return this.weight;
    }
    public String getFlightNum(){
        return this.flightNumber;
    }
    
}
