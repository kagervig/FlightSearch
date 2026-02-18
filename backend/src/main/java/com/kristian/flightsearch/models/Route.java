package com.kristian.flightsearch.models;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class Route {
    private String[] airports;
    private ArrayList<ArrayList<Flight>> flights;
    private int cheapestTotalPrice;

    /*flights is a double arraylist as it stores all of the options per leg
    For example: LHR-->JFK is a leg, but there may be n options to pick from
    so for a route LHR-->JFK-->FCO-->LHR there could be 3 legs * 5 flights per leg to pick from */

    public Route(String[] airports, ArrayList<ArrayList<Flight>> flights){
        this.airports = airports;
        this.flights = flights;
        cheapestTotalPrice = 0;

        //calculate cheapest total price for all flights
        for (int i = 0; i < flights.size(); i++){
            int legPrices[] = new int[flights.get(i).size()];
            int j = 0;
            for (Flight f : flights.get(i)){
                legPrices[j] = f.getPrice();
                j++;
            }
            //System.out.println("");
            int cheapestLegPrice = legPrices[0];

            for (int price : legPrices){
                if (price < cheapestLegPrice){
                    cheapestLegPrice = price;
                }
            }
            cheapestTotalPrice += cheapestLegPrice;
        }
    }

    public void printRoutes(Route r){
        String printedRoute = "";
        for (int i = 0; i < airports.length; i++){
            printedRoute += airports[i];
            if (i == airports.length - 1){
                printedRoute += "-->";
            }
        }
        System.out.println(printedRoute);
    }

    public void printRoutesWithPrices(Route r){
        String printedRoute = "";
        for (int i = 0; i < airports.length; i++){
            printedRoute += airports[i];
            if (i != airports.length - 1){
                printedRoute += "-->";
            }
        }
        System.out.println(printedRoute + " Total Price: $" + r.getCheapestTotalPrice());
    }

    public static void main(String[] args) {
        Airport yyz = new Airport("YYZ", "Toronto Pearson", 43.67, -79.63, "America/Toronto", 11120, "Toronto", "Canada");
        Airport jfk = new Airport("JFK", "John F Kennedy", 40.64, -73.78, "America/New_York", 14511, "New York", "USA");
        Airport lax = new Airport("LAX", "Los Angeles Intl", 33.94, -118.41, "America/Los_Angeles", 12091, "Los Angeles", "USA");

        String[] airports = {"YYZ", "JFK", "LAX", "YYZ"};

        // Leg 1: YYZ -> JFK (2 options)
        Flight yyzJfk1 = new Flight(yyz, jfk, 550, LocalTime.of(8, 0), "AC 1001");
        yyzJfk1.setPrice(200);
        Flight yyzJfk2 = new Flight(yyz, jfk, 550, LocalTime.of(14, 0), "AC 1002");
        yyzJfk2.setPrice(150);  // cheaper

        // Leg 2: JFK -> LAX (3 options)
        Flight jfkLax1 = new Flight(jfk, lax, 3974, LocalTime.of(9, 0), "AA 2001");
        jfkLax1.setPrice(400);
        Flight jfkLax2 = new Flight(jfk, lax, 3974, LocalTime.of(13, 0), "DL 2002");
        jfkLax2.setPrice(350);  // cheapest
        Flight jfkLax3 = new Flight(jfk, lax, 3974, LocalTime.of(18, 0), "UA 2003");
        jfkLax3.setPrice(375);

        // Leg 3: LAX -> YYZ (2 options)
        Flight laxYyz1 = new Flight(lax, yyz, 3500, LocalTime.of(10, 0), "AC 3001");
        laxYyz1.setPrice(500);
        Flight laxYyz2 = new Flight(lax, yyz, 3500, LocalTime.of(19, 0), "WN 3002");
        laxYyz2.setPrice(380);  // cheaper

        ArrayList<ArrayList<Flight>> legOptions = new ArrayList<>();
        legOptions.add(new ArrayList<>(List.of(yyzJfk1, yyzJfk2)));
        legOptions.add(new ArrayList<>(List.of(jfkLax1, jfkLax2, jfkLax3)));
        legOptions.add(new ArrayList<>(List.of(laxYyz1, laxYyz2)));

        // Expected cheapest total: 150 + 350 + 380 = 880
        Route route = new Route(airports, legOptions);
        System.out.println("Cheapest total price: $" + route.getCheapestTotalPrice());
        System.out.println("Expected: $880");
    }


    public String[] getAirports(){
        return this.airports;
    }
    public ArrayList<ArrayList<Flight>> getFlights(){
        return this.flights;
    }
    public int getCheapestTotalPrice(){
        return this.cheapestTotalPrice;
    }
    public void setCheapestTotalPrice(int cheapestTotalPrice){
        this.cheapestTotalPrice = cheapestTotalPrice;
    }

    
}
