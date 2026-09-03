package com.kristian.flightsearch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import com.kristian.flightsearch.db.DatabaseManager;
import com.kristian.flightsearch.db.RouteStore;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.models.FlightResult;
import com.kristian.flightsearch.utils.FlightPrinter;

/** Terminal runner for building and testing route queries before they become API endpoints. */
public class RouteBuilder {

    public static void main(String[] args) {
        Scanner scnr = new Scanner(System.in);
        boolean goingHome = false;
        int ticketPrice = 0;
        FlightPrinter fp = new FlightPrinter();


        DatabaseManager.initialize();
        try (Connection conn = DatabaseManager.getDataSource().getConnection()) {

            String homeAirport = null;
            while (homeAirport == null) {
                System.out.print("Enter home airport code (or 'quit' to exit): ");
                String input = scnr.nextLine().toUpperCase();
                if (input.equals("QUIT")) return;
                if (RouteStore.validateAirport(input, conn)) {
                    homeAirport = input;
                } else {
                    System.out.println("Airport not found: " + input + ". Try again.");
                }
            }

            ArrayList<Flight> route = new ArrayList<>();

            if (RouteStore.validateAirport(homeAirport, conn)) {
                String currentLocation = homeAirport;

                while (!goingHome) {
                    System.out.println("\nCheapest flights departing from " + currentLocation);
                    ArrayList<FlightResult> fr = RouteStore.findFlights(currentLocation, conn);
                    printFlightResults(fr);

                    System.out.print("\nMake a selection: ");
                    System.out.print("\n0 to search for flights home: ");
                    int selection = scnr.nextInt();
                    while (selection < 0 || selection > fr.size()) {
                        System.out.print("Make a valid selection (0-" + fr.size() + "): ");
                        selection = scnr.nextInt();
                    }

                    if (selection == 0) {
                        fr = RouteStore.findFlightHome(currentLocation, homeAirport, conn);
                        if (fr == null || fr.isEmpty()) {
                            System.out.println("No direct flights home from " + currentLocation + " — pick another destination first.");
                        } else {
                            goingHome = true;
                            printFlightResults(fr);
                            System.out.print("\nMake a selection: ");
                            selection = scnr.nextInt();
                            while (selection < 0 || selection > fr.size()) {
                                System.out.print("Make a valid selection (1-" + fr.size() + "): ");
                                selection = scnr.nextInt();
                            }
                            Flight chosen = RouteStore.convertToFlight(fr.get(selection - 1), conn);
                            route.add(chosen);
                        }
                    } else {
                        Flight chosen = RouteStore.convertToFlight(fr.get(selection - 1), conn);
                        route.add(chosen);
                        currentLocation = fr.get(selection - 1).destination();
                    }
                }

                System.out.println("\nNumber of legs: " + route.size());
                for (Flight f: route){
                    fp.print(f);
                    ticketPrice += f.getPrice();
                }
                System.out.println("");
                System.out.println("Total Ticket Price: $" + ticketPrice);
            } else {
                System.out.println("Airport not found: " + homeAirport);
            }

        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }


      
        
    }

    public static void printFlightResults(ArrayList<FlightResult> results){
        System.out.printf("%-4s %-8s %-13s %-10s %s%n", "#", "Price", "Destination", "Flight", "Airline");
        System.out.println("-".repeat(49));
        for (int i = 0; i < results.size(); i++) {
            FlightResult f = results.get(i);
            System.out.printf("%-4s %-8s %-13s %-10s %s%n",
                (i + 1), "$" + f.price(), f.destination(), f.flightNumber(), f.airline());
        }
        System.out.printf("%-4s %-8s %n", 0, "Return Home");
    }

 
}
