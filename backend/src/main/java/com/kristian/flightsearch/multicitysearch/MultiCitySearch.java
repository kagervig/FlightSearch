
package com.kristian.flightsearch.multicitysearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.Route;
import com.kristian.flightsearch.datagenerator.FSFileReader;
import com.kristian.flightsearch.utils.FlightPrinter;

public class MultiCitySearch {
    private static final FSFileReader fileReader = new FSFileReader("609airports.txt");
    private static final Airport[] airports = fileReader.getAirports();
    private static final HashMap<String, Flight> flightList = FlightReader.readFlights("flights.txt", airports);
    private static final HashMap<String, ArrayList<Flight>> flightIndex = FlightGenerator.flightMapper(flightList);

    /**
     * Searches for all valid multi-city routes, sorted by cheapest total price.
     *
     * @param homeAirport  The origin/return airport code (e.g. "YYZ")
     * @param destinations Array of destination airport codes to visit
     * @return Sorted list of valid routes (cheapest first), empty if none found
     */
    public static ArrayList<Route> search(String homeAirport, String[] destinations) {
        ArrayList<String[]> combinations = flightCombinations(destinations, homeAirport);

        // remove routes where any leg has no available flight
        for (int i = combinations.size() - 1; i >= 0; i--) {
            if (!hasFlightsForAllLegs(combinations.get(i), flightIndex)) {
                combinations.remove(i);
            }
        }

        ArrayList<Route> validRoutes = new ArrayList<>();
        for (int i = 0; i < combinations.size(); i++) {
            String[] route = combinations.get(i);
            ArrayList<ArrayList<Flight>> routeFlights = new ArrayList<>();
            for (int j = 0; j < route.length - 1; j++) {
                ArrayList<Flight> legFlights = FlightGenerator.flightRouteSearch(flightIndex, route[j], route[j + 1]);
                if (legFlights != null) {
                    routeFlights.add(legFlights);
                } else {
                    routeFlights.add(new ArrayList<>());
                }
            }
            validRoutes.add(new Route(route, routeFlights));
        }

        // sort by cheapest total price
        validRoutes.sort((a, b) -> Integer.compare(a.getCheapestTotalPrice(), b.getCheapestTotalPrice()));
        return validRoutes;
    }

    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);
        String homeAirport;
        int numCitiesToVisit = 0;
        String input;
        FlightPrinter fp = new FlightPrinter();

        System.out.println("Enter your home airport: ");
        homeAirport = scnr.nextLine().trim().toUpperCase();
        System.out.println("How many cities will you visit?");
        while (true) {
            System.out.println("Min 1, Max 5");
            input = scnr.nextLine();
            try {
                numCitiesToVisit = Integer.parseInt(input);
                if (numCitiesToVisit < 1 || numCitiesToVisit > 5) {
                    System.out.println("Please enter a number between 1 and 5.");
                    continue;
                }
                break;
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }

        String[] airportsToVisit = new String[numCitiesToVisit];
        airportsToVisit = captureAirports(numCitiesToVisit, homeAirport); // capture all airports to visit from user

        ArrayList<String[]> flightCombinations = new ArrayList<>(); // stores all possible route combinations
        flightCombinations = flightCombinations(airportsToVisit, homeAirport); // permutes all possible route
                                                                               // combinations

        // prints permuted routes when prompted
        System.out.println("Print routes? Y/N");
        String choice = scnr.nextLine().trim().toUpperCase();
        if (choice.equals("Y")) {
            printRoutes(flightCombinations);
        }

        // checking to see whether all routes are possible
        // remove any impossible routes

        System.out.println("Total permuted routes: " + flightCombinations.size());

        for (int i = flightCombinations.size() - 1; i >= 0; i--) {
            if (!hasFlightsForAllLegs(flightCombinations.get(i), flightIndex)) {
                flightCombinations.remove(i);
            }
        }

        // populate ArrayList with all routes that are served by a flight
        ArrayList<Route> validRoutes = new ArrayList<>();

        for (int i = 0; i < flightCombinations.size(); i++) {
            String[] route = flightCombinations.get(i); // get the route's airports(strings) into an array
            ArrayList<ArrayList<Flight>> routeFlights = new ArrayList<>(); // store flight objects per leg of the route
                                                                           // may be multiple flight options per leg,
                                                                           // hence ArrL<ArrL<Flight>>
            for (int j = 0; j < route.length - 1; j++) { // iterate through each leg of the route
                ArrayList<Flight> legFlights = FlightGenerator.flightRouteSearch(flightIndex, route[j], route[j + 1]);
                // search flight index for routes between these airports
                if (legFlights != null) {
                    routeFlights.add(legFlights);
                } else {
                    routeFlights.add(new ArrayList<>());
                }
            }
            validRoutes.add(new Route(route, routeFlights));
        }


        Route cheapestRoute = findCheapestRoute(validRoutes);
        System.out.println("Total valid routes: " + flightCombinations.size());
        System.out.println("Cheapest route: ");
        cheapestRoute.printRoutesWithPrices(cheapestRoute);
        System.out.println("");

        System.out.println("Print flight details? Y/N");
        choice = scnr.nextLine().trim().toUpperCase();
        if (choice.equals("Y")) {
            for (ArrayList<Flight> legFlights : cheapestRoute.getFlights()){
                for (Flight f : legFlights) {
                    fp.print(f);
                }
            }
        }
        

        // prints permuted routes when prompted
        System.out.println("Print all other route options? Y/N");
        choice = scnr.nextLine().trim().toUpperCase();
        if (choice.equals("Y")) {
            for (Route r : validRoutes) {
                r.printRoutesWithPrices(r);
            }
        }

    }

    public static void printRoutes(ArrayList<String[]> flightCombinations) {
        int i = 1;
        for (String[] s : flightCombinations) {
            System.out.print("Route " + i + ": ");
            for (int j = 0; j < s.length; j++) {
                System.out.print(s[j]);
                if (j < s.length - 1) {
                    System.out.print("-->");
                }
            }
            i++;
            System.out.println("");
        }
    }

    public static Route findCheapestRoute(ArrayList<Route> validRoutes){
        Route cheapestRoute = validRoutes.get(1);
        for (Route r : validRoutes){
            if (r.getCheapestTotalPrice() < cheapestRoute.getCheapestTotalPrice()){
                cheapestRoute = r;
            }
        }
        return cheapestRoute;
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

    // check if a flight exists that connects each airport on the route
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
                    continue;
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