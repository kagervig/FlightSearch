
package com.kristian.flightsearch.flightsearch;

import java.io.FileReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.GraphTraverser;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.datagenerator.FSFileReader;

public class flightSearch {
    private static final FSFileReader fileReader = new FSFileReader("top100global.txt");
    private static final Airport[] airports = fileReader.getAirports();
    private static final HashMap<String, Flight> flightList = FlightReader.readFlights("flights.txt", airports);
    private static final HashMap<String, ArrayList<Flight>> flightIndex = FlightGenerator.flightMapper(flightList);

    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);
        String homeAirport;
        int citiesToVisit = 0;

        System.out.println("Enter your home airport: ");
        homeAirport = scnr.nextLine().trim().toUpperCase();
        System.out.println("How many cities do you want to visit?");
        System.out.println("Min 1, Max 5");
        citiesToVisit = scnr.nextInt();
        scnr.nextLine();
        String[] airportsToVisit = new String[citiesToVisit];
        airportsToVisit = captureAirports(citiesToVisit, homeAirport);

        ArrayList<String[]> flightCombinations = new ArrayList<>();
        flightCombinations = flightCombinations(airportsToVisit, homeAirport);


        //prints permuted routes when prompted
        System.out.println("Print routes? Y/N");
        String choice = scnr.nextLine().trim().toUpperCase();
        if (choice.equals("Y")){
            printRoutes(flightCombinations);
        }

        //checking to see whether all routes are possible
        //remove any impossible routes

        
        System.out.println("Total permuted routes: " + flightCombinations.size());

        for (int i = flightCombinations.size() - 1; i >= 0; i--) {
            if (!hasFlightsForAllLegs(flightCombinations.get(i), flightIndex)) {
                flightCombinations.remove(i);
            }
        }


        System.out.println("Total valid routes: " + flightCombinations.size());


        //prints permuted routes when prompted
        System.out.println("Print routes? Y/N");
        choice = scnr.nextLine().trim().toUpperCase();
        if (choice.equals("Y")){
            printRoutes(flightCombinations);
        }


        
        

    }

    public static void printRoutes(ArrayList<String[]> flightCombinations){
        int i = 1;
        for (String[] s : flightCombinations) {
            System.out.print("Route " + i + ": ");
            for (String t : s) {
                System.out.print(t + "-->");
            }
            i++;
            System.out.println("");
        }
    }



    public static ArrayList<String[]> flightCombinations(String[] airportsToVisit, String homeAirport) {
        ArrayList<String[]> flightCombinations = new ArrayList<>();

        // first and last airport always the same
        // edge case - only one airport to visit.
        if (airportsToVisit.length == 1) {
            String[] flightRoute = { homeAirport, airportsToVisit[0], homeAirport };
            flightCombinations.add(flightRoute);
            return flightCombinations;
        }

        // Generate all permutations of citiesToVisit
        permuteRoutes(airportsToVisit, 0, flightCombinations, homeAirport);
        return flightCombinations;
    }


    //check if a flight exists that connects each airport on the route
    public static boolean hasFlightsForAllLegs(String[] flightRoute, HashMap<String, ArrayList<Flight>> flightIndex) {
        int numFlights = flightRoute.length;
        for (int i = 0; i < numFlights - 1; i++) {
            String key = flightRoute[i] + flightRoute[i + 1];
            if (!flightIndex.containsKey(key)) {
                return false; // If any leg is missing, route is not possible
            }
        }
        return true; // All legs exist
    }

    // Helper method to generate permutations and add routes
    private static void permuteRoutes(String[] cities, int start, ArrayList<String[]> routes, String home) {
        // Base case: if we've fixed all positions, add the route (home -> permutation
        // -> home)
        if (start == cities.length) {
            // Create a new route array with home at start and end
            String[] route = new String[cities.length + 2];
            route[0] = home; // Start at home airport
            System.arraycopy(cities, 0, route, 1, cities.length); // Add current permutation
            route[route.length - 1] = home; // End at home airport
            routes.add(route); // Add this route to the list
        } else {
            // Recursively generate all permutations by swapping each element into the
            // current position
            for (int i = start; i < cities.length; i++) {
                swap(cities, start, i); // Swap to fix one city at the current position
                permuteRoutes(cities, start + 1, routes, home); // Recurse for the next position
                swap(cities, start, i); // Backtrack: undo the swap
            }
        }
    }

    // Helper method to swap elements in array
    private static void swap(String[] arr, int i, int j) {
        String temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * Prompts the user to enter a dynamic number of airport codes based on
     * citiesToVisit.
     * 
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
                if (!isValidAirportEntry(choice)) {
                    System.out.println("Airport code invalid, try again.");
                    continue;
                }
                if (!fileReader.isValidAirportCode(choice)) {
                    System.out.println("Please pick a supported airport");
                }
                airports[i] = choice;
                break;
            }
        }
        return airports;
    }

    private static boolean isValidAirportEntry(String code) {
        return code != null && code.matches("[A-Za-z]{3}");
    }

}

/*
 * This function will take in the users home airport, and 5 desired destination
 * airports.
 * Not using a graph, assumes n days gap between flights for each of the 120
 * iterations.
 * Looks for the cheapest flight between each airport pair.
 * 
 * Steps
 * 1) generate 120 permutations of the airport flight combinations
 * 2) remove those routes that aren't possible
 * 3) for each possible route, find cheapest flight
 * 
 */