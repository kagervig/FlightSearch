package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airport;

public class FlightDistanceCalculator {

    public static double calcDistance(Airport airport1, Airport airport2){
        double lat1 = airport1.getLat();
        double lat2 = airport2.getLat();
        double long1 = airport1.getLon();
        double long2 = airport2.getLon();
        double distance = 0.0;
        final int EARTH_RADIUS = 6371; //6,371km
        double a, c;

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double long1Rad = Math.toRadians(long1);
        double long2Rad = Math.toRadians(long2);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLong = long2Rad - long1Rad;

        a = ((Math.sin((deltaLat / 2.0))) * (Math.sin((deltaLat / 2.0)))) 
        + Math.cos(lat1Rad) * Math.cos(lat2Rad) 
        * (Math.sin(deltaLong / 2.0)) 
        * (Math.sin(deltaLong / 2.0));

        c = (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
        distance = EARTH_RADIUS * c;

        return distance;
    }




    public static void main(String[] args) {
        double distance = 0.0;
        Airport JFK = new Airport("JFK", "John F. Kennedy International Airport", 40.641766, -73.780968, "EST");
        Airport DEN = new Airport("DEN", "Denver International Airport", 39.849312, -104.673828, "EST");
        distance = calcDistance(JFK, DEN);
        distance *= 0.621371;

        

        System.out.printf("Distance between %s and %s is: %.0f nm", JFK.getCode(), DEN.getCode(), distance);
    }
}


/* 
Given:

Point 1: (lat₁, lon₁)
Point 2: (lat₂, lon₂)
Earth's radius: R = 6,371 km (or 3,959 miles)

Formula:
a = sin²(Δlat/2) + cos(lat₁) × cos(lat₂) × sin²(Δlon/2)
c = 2 × atan2(√a, √(1−a))
distance = R × c
Where:

Δlat = lat₂ - lat₁
Δlon = lon₂ - lon₁
All angles must be in radians

//point 1 is JFK
double lat1 = 40.641766;
double long1 = -73.780968;
//point 2 is LAX
double lat2 = 33.942791;
double long2 = -118.410042;


TO DO
create a graph with 5 airports, each with 1 flight per day between all of them.
can i search and find all possible routes from 1 airport to another.



*/