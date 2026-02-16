package com.kristian.flightsearch.models;

public class Airport {
    String name;
    String code;
    double lat;
    double lon;
    String timeZone;
    int runwayLength;
    String city;
    String country;


    public Airport(String code, String name, double lat, double lon, String timeZone, int runwayLength, String city, String country){
        this.name = name;
        this.code = code;
        this.lat = lat;
        this.lon = lon;
        this.timeZone = timeZone;
        this.runwayLength = runwayLength;
        this.city = city;
        this.country = country;
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
    public int getRunwayLength(){
        return this.runwayLength;
    }
    public String getCity(){
        return this.city;
    }
    public String getCountry(){
        return this.country;
    }
    
}
