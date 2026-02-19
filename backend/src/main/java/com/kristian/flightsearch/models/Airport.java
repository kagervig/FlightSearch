package com.kristian.flightsearch.models;

public class Airport {
    String name;
    String code;
    double lat;
    double lon;
    int runwayLength;
    int elevation;
    String city;
    String country;


    public Airport(String code, String name, double lat, double lon, int runwayLength, int elevation, String city, String country){
        this.name = name;
        this.code = code;
        this.lat = lat;
        this.lon = lon;
        this.runwayLength = runwayLength;
        this.elevation = elevation;
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
    public int getElevation(){
        return this.elevation;
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
