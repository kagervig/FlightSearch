package com.kristian.flightsearch;

import java.util.*;
import java.time.Duration;

import com.kristian.flightsearch.utils.FlightPrinter;
import com.kristian.flightsearch.datagenerator.FSFileReader;
import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.datagenerator.FlightWriter;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.Dijkstra;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.flightgraph.GraphTraverser;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class Main {
    public static void main(String[] args) {
        String filepath = "flights.txt";
        HashMap<String, Flight> flightList = new HashMap<String, Flight>(); // hashmap list of flights
        FlightPrinter fp = new FlightPrinter(); // needed to print flight data
        FlightGraph flightNetwork = new FlightGraph(true, true); // instantiate new flightgraph
        ArrayList<AirportVertex> airportVertices = new ArrayList<AirportVertex>(); // declare arraylist of
                                                                                   // airportvertices

        FSFileReader fileReader = new FSFileReader("top100global.txt");
        Airport[] airports = fileReader.getAirports();
        for (Airport a : airports) {
            airportVertices.add(flightNetwork.addVertex(a));
        }

        //FlightWriter.writeFlights(filepath, 15000, "top100global.txt"); // optional: generate and write flights to file
        flightList = FlightReader.readFlights(filepath, airports);

        HashMap<String, ArrayList<Flight>> flightIndex = FlightGenerator.flightMapper(flightList);

        // Iterate through flightIndex and add edges
        for (ArrayList<Flight> flights : flightIndex.values()) {
            for (Flight f : flights) {
                AirportVertex origin = flightNetwork.getVertex(f.getOrigin().getCode());
                AirportVertex dest = flightNetwork.getVertex(f.getDestination().getCode());

                if (origin != null && dest != null) {
                    flightNetwork.addEdge(origin, dest, f.getPrice(), f.getDuration(), f.getFlightNumber());
                }
            }
        }
        flightNetwork.print();
        System.out.println("");
        menu(flightIndex, flightList, flightNetwork, fileReader);

    }

    public static void menu(HashMap<String, ArrayList<Flight>> flightIndex, HashMap<String, Flight> flightList,
            FlightGraph flightNetwork, FSFileReader fileReader) {
        Scanner scanner = new Scanner(System.in);
        String choice = "";

        while (!choice.equals("0")) {
            System.out.println("\n===== FLIGHT SEARCH MENU =====");
            System.out.println("1. View Route Counter");
            System.out.println("2. Search Flight by Number");
            System.out.println("3. Search Flights by Route");
            System.out.println("4. Depth First Traversal");
            System.out.println("5. Breadth First Traversal");
            System.out.println("6. Dijkstra Price Search");
            System.out.println("7. Dijkstra Distance Search");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            System.out.flush();

            choice = scanner.nextLine();

            ArrayList<AirportVertex> visitedVertices;
            String origin;
            String destination;
            AirportVertex originVertex;
            AirportVertex destVertex;

            switch (choice) {
                case "1":
                    FlightGenerator.routeCounter(flightIndex);
                    break;
                case "2":
                    FlightGenerator.flightNumSearch(flightList);
                    break;
                case "3":
                    FlightGenerator.flightRouteSearch(flightIndex);
                    break;
                case "4":
                    origin = getValidAirportCode(scanner, fileReader, "Enter Origin Airport: ");
                    if (origin == null) break;
                    destination = getValidAirportCode(scanner, fileReader, "Enter Destination Airport: ");
                    if (destination == null) break;

                    originVertex = flightNetwork.getVertex(origin);
                    destVertex = flightNetwork.getVertex(destination);
                    visitedVertices = new ArrayList<AirportVertex>();

                    GraphTraverser.depthFirstTraversal(originVertex, destVertex, visitedVertices, 0, "",
                            Duration.ZERO);
                    break;
                case "5":
                    origin = getValidAirportCode(scanner, fileReader, "Enter Origin Airport: ");
                    if (origin == null) break;
                    destination = getValidAirportCode(scanner, fileReader, "Enter Destination Airport: ");
                    if (destination == null) break;

                    originVertex = flightNetwork.getVertex(origin);
                    destVertex = flightNetwork.getVertex(destination);
                    visitedVertices = new ArrayList<AirportVertex>();

                    GraphTraverser.breadthFirstSearch(originVertex, destVertex, visitedVertices);
                    break;
                case "6":
                    origin = getValidAirportCode(scanner, fileReader, "Enter Origin Airport: ");
                    if (origin == null) break;

                    Map[] priceDictionary = Dijkstra.searchByPrice(flightNetwork, flightNetwork.getVertex(origin));
                    Dijkstra.printSearchResult(priceDictionary);
                    break;
                case "7":
                    origin = getValidAirportCode(scanner, fileReader, "Enter Origin Airport: ");
                    if (origin == null) break;

                    Map[] durationDictionary = Dijkstra.searchByDuration(flightNetwork,
                            flightNetwork.getVertex(origin));
                    Dijkstra.printSearchResult(durationDictionary);
                    break;
                case "0":
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }

    private static String getValidAirportCode(Scanner scanner, FSFileReader fileReader, String prompt) {
        while (true) {
            System.out.print(prompt);
            System.out.flush();
            String input = scanner.nextLine().toUpperCase();

            if (input.equals("0")) {
                return null;
            }

            if (fileReader.isValidAirportCode(input)) {
                return input;
            }

            System.out.println("Invalid airport code '" + input + "'. Enter 0 to cancel.");
        }
    }

    public static Airport[] getAirports(String filePath) {
        FSFileReader fr = new FSFileReader(filePath);
        return fr.getAirports();
    }

}
