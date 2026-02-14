package com.kristian.flightsearch.models;

public class Airport {
    String name;
    String code;
    double lat;
    double lon;
    String timeZone;


    public Airport(String code, String name, double lat, double lon, String timeZone){
        this.name = name;
        this.code = code;
        this.lat = lat;
        this.lon = lon;
        this.timeZone = timeZone;
    }
    public String getName(){
        return this.name;
    }
    public String getCode(){
        return this.code;
    }
    public double getLat(){
        return this.lat;
    }
    public double getLon(){
        return this.lon;
    }
    public String getTimeZone(){
        return this.timeZone;
    }
    
}
