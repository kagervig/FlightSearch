package com.kristian.flightsearch.utils;

import com.kristian.flightsearch.models.Airport;

public class AirportPrinter {
    
    Airport airport;

    public void airportPrinter(Airport airport){
        this.airport = airport;
    }

    public void print(Airport airport){
        System.out.println("Airport: " + airport.getCode());
        System.out.println(airport.getName());
        System.out.println("Time Zone: " + airport.getTimeZone());
        System.out.println("");
    }

    public static void main(String[] args) {
        Airport ATL = new Airport("ATL","Hartsfield-Jackson Atlanta International Airport",33.6404,-84.4199,"EST");

        AirportPrinter ap = new AirportPrinter();
        ap.print(ATL);
    }
}
