package com.kristian.flightsearch.models;

public class Airport {
    String name;
    String code;
    double lat;
    double lon;
    int runwayLengthFt;
    int elevation;
    String city;
    String country;
    String icaoCode;
    String timezone;
    double utcOffset;

    public Airport(String code, String name, double lat, double lon, int runwayLengthFt, int elevation, String city, String country) {
        this.name = name;
        this.code = code;
        this.lat = lat;
        this.lon = lon;
        this.runwayLengthFt = runwayLengthFt;
        this.elevation = elevation;
        this.city = city;
        this.country = country;
    }

    public Airport(String code, String name, double lat, double lon, int runwayLengthFt, int elevation, String city, String country, String icaoCode, String timezone, double utcOffset) {
        this(code, name, lat, lon, runwayLengthFt, elevation, city, country);
        this.icaoCode = icaoCode;
        this.timezone = timezone;
        this.utcOffset = utcOffset;
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
    public int getRunwayLengthFt(){
        return this.runwayLengthFt;
    }
    // Alias kept for compatibility until all call sites are updated
    public int getRunwayLength(){
        return this.runwayLengthFt;
    }
    public String getCity(){
        return this.city;
    }
    public String getCountry(){
        return this.country;
    }
    public String getIcaoCode(){
        return this.icaoCode;
    }
    public String getTimezone(){
        return this.timezone;
    }
    public double getUtcOffset(){
        return this.utcOffset;
    }
}
