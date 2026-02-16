
package com.kristian.flightsearch.flightsearch;

import java.io.FileReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.GraphTraverser;

public class flightSearch {
    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);
        String homeAirport;
        int citiesToVisit = 0;

        System.out.println("Enter your home airport: ");
        homeAirport = scnr.nextLine();
        System.out.println("How many cities do you want to visit?");
        System.out.println("Min 1, Max 5");
        citiesToVisit = scnr.nextInt();
        String[] airportsToVisit = new String[citiesToVisit];
        airportsToVisit = captureAirports(citiesToVisit, homeAirport);




        
        


        


    }

    public ArrayList<String[]> flightCombinations(String[] citiesToVisit, String homeAirport){
        ArrayList<String[]> flightCombinations = new ArrayList<>();

        //first and last airport always the same
        //edge case - only one airport to visit.
        if (citiesToVisit.length == 1){
            String[] flightRoute = {homeAirport, citiesToVisit[0], homeAirport};
            flightCombinations.add(flightRoute);
            return flightCombinations;
        }

        // Generate all permutations of citiesToVisit
        permuteRoutes(citiesToVisit, 0, flightCombinations, homeAirport);
        return flightCombinations;
    }

    // Helper method to generate permutations and add routes
    private void permuteRoutes(String[] cities, int start, ArrayList<String[]> routes, String home) {
        if (start == cities.length) {
            String[] route = new String[cities.length + 2];
            route[0] = home;
            System.arraycopy(cities, 0, route, 1, cities.length);
            route[route.length - 1] = home;
            routes.add(route);
        } else {
            for (int i = start; i < cities.length; i++) {
                swap(cities, start, i);
                permuteRoutes(cities, start + 1, routes, home);
                swap(cities, start, i);
            }
        }
    }

    // Helper method to swap elements in array
    private void swap(String[] arr, int i, int j) {
        String temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }


        /**
     * Prompts the user to enter a dynamic number of airport codes based on citiesToVisit.
     * @param citiesToVisit The number of cities to visit.
     * @return An array of Strings containing the entered airport codes.
     */
    public static String[] captureAirports(int citiesToVisit, String homeAirport) {
        Scanner scanner = new Scanner(System.in);
        String[] airports = new String[citiesToVisit];
        String choice = "";
        for (int i = 0; i < citiesToVisit; i++) {
            while (true) {
                System.out.print("Enter airport code for city " + (i + 1) + ": ");
                choice = scanner.nextLine().trim().toUpperCase();
                if (choice.equals(homeAirport)) {
                    System.out.println("Invalid choice, cannot be the same as the home city");
                    continue;
                }
                if (!isValidAirportCode(choice)) {
                    System.out.println("Invalid choice, must be a valid airport code");
                    continue;
                }
                airports[i] = choice;
                break;
            }
        }
        return airports;
    }

    private static boolean isValidAirportCode(String code) {
        return code != null && code.matches("[A-Za-z]{3}");
    }

     




    
}




/*
This function will take in the users home airport, and 5 desired destination airports.
Not using a graph, assumes n days gap between flights for each of the 120 iterations. 
Looks for the cheapest flight between each airport pair.

Steps
1) generate 120 permutations of the airport flight combinations
2) remove those routes that aren't possible
3) for each possible route, find cheapest flight

*/