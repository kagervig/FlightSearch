package com.kristian.flightsearch.datagenerator;

import java.util.ArrayList;
import java.util.HashMap;

import com.kristian.flightsearch.models.Flight;

public class FlightGenerator {

    public static HashMap<String, ArrayList<Flight>> flightMapper(HashMap<String, Flight> flightList) {

        HashMap<String, ArrayList<Flight>> flightIndex = new HashMap();

        // creates a hashmap of flight route
        // key is a string (E.G. JFKSEA)
        // value is arraylist of Flight objects serving that route
        // iterate through flightList, for each flight:
        // concatenate origin & dest as key.
        // if origin-destination are the same, add to arraylist

        for (Flight f : flightList.values()) {
            String route = f.getOrigin().getCode() + f.getDestination().getCode();
            // System.out.println(route);
            flightIndex.putIfAbsent(route, new ArrayList<>());
            flightIndex.get(route).add(f);
        }

        return flightIndex;
    }

    public static ArrayList<Flight> flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex, String origin,
            String destination) {
        ArrayList<Flight> temp = flightIndex.get(origin + destination);
        return temp;
    }

}

