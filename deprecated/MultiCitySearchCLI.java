package com.kristian.flightsearch.multicitysearch;

import java.util.ArrayList;
import java.util.Scanner;

import com.kristian.flightsearch.db.AirportStore;
import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.db.FlightStore;
import com.kristian.flightsearch.models.Route;
import com.kristian.flightsearch.utils.FlightPrinter;

/*
 * CLI entry point for multi-city search. Provides interactive prompts for
 * local development and testing without the web server.
 */
public class MultiCitySearchCLI {

    public static void main(String[] args) {
        DatabaseManager.initialize();
        AirportStore airportStore = new AirportStore(DatabaseManager.getDataSource());
        FlightStore flightStore = new FlightStore(DatabaseManager.getDataSource(), airportStore);
        MultiCitySearch mcs = new MultiCitySearch(airportStore, flightStore);

        Scanner scnr = new Scanner(System.in);
        FlightPrinter fp = new FlightPrinter();

        System.out.println("Enter your home airport: ");
        String homeAirport = scnr.nextLine().trim().toUpperCase();

        System.out.println("How many cities will you visit?");
        int numCitiesToVisit = 0;
        while (true) {
            System.out.println("Min 1, Max 5");
            String input = scnr.nextLine();
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

        String[] airportsToVisit = captureAirports(numCitiesToVisit, homeAirport, airportStore, scnr);

        ArrayList<String[]> flightCombinations = MultiCitySearch.flightCombinations(airportsToVisit, homeAirport);

        System.out.println("Print routes? Y/N");
        if (scnr.nextLine().trim().toUpperCase().equals("Y")) {
            printRoutes(flightCombinations);
        }

        System.out.println("Total permuted routes: " + flightCombinations.size());

        ArrayList<Route> validRoutes = mcs.search(homeAirport, airportsToVisit);

        Route cheapestRoute = findCheapestRoute(validRoutes);
        System.out.println("Total valid routes: " + validRoutes.size());
        System.out.println("Cheapest route: ");
        cheapestRoute.printRoutesWithPrices(cheapestRoute);
        System.out.println("");

        System.out.println("Print flight details? Y/N");
        if (scnr.nextLine().trim().toUpperCase().equals("Y")) {
            for (var legFlights : cheapestRoute.getFlights()) {
                for (var f : legFlights) {
                    fp.print(f);
                }
            }
        }

        System.out.println("Print all other route options? Y/N");
        if (scnr.nextLine().trim().toUpperCase().equals("Y")) {
            for (Route r : validRoutes) {
                r.printRoutesWithPrices(r);
            }
        }
    }

    private static String[] captureAirports(int citiesToVisit, String homeAirport, AirportStore airportStore, Scanner scanner) {
        String[] airports = new String[citiesToVisit];
        for (int i = 0; i < citiesToVisit; i++) {
            while (true) {
                System.out.print("Enter airport code for city " + (i + 1) + ": ");
                String choice = scanner.nextLine().trim().toUpperCase();
                if (choice.equals(homeAirport)) {
                    System.out.println("Invalid choice, cannot be the same as the home city");
                    continue;
                }
                if (!isValidAirportEntry(choice)) {
                    System.out.println("Airport code invalid, try again.");
                    continue;
                }
                if (!airportStore.isValidAirportCode(choice)) {
                    System.out.println("Please pick a supported airport");
                    continue;
                }
                airports[i] = choice;
                break;
            }
        }
        return airports;
    }

    private static void printRoutes(ArrayList<String[]> flightCombinations) {
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

    private static Route findCheapestRoute(ArrayList<Route> validRoutes) {
        Route cheapestRoute = validRoutes.get(0);
        for (Route r : validRoutes) {
            if (r.getCheapestTotalPrice() < cheapestRoute.getCheapestTotalPrice()) {
                cheapestRoute = r;
            }
        }
        return cheapestRoute;
    }

    private static boolean isValidAirportEntry(String code) {
        return code != null && code.matches("[A-Za-z]{3}");
    }
}
