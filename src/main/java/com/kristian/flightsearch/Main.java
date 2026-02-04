package com.kristian.flightsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.kristian.flightsearch.utils.FlightPrinter;
import com.kristian.flightsearch.datagenerator.FileReader;
import com.kristian.flightsearch.datagenerator.FlightGenerator;
import com.kristian.flightsearch.datagenerator.FlightReader;
import com.kristian.flightsearch.datagenerator.FlightWriter;
import com.kristian.flightsearch.flightgraph.AirportVertex;
import com.kristian.flightsearch.flightgraph.FlightGraph;
import com.kristian.flightsearch.flightgraph.GraphTraverser;
import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class Main {
    public static void main(String[] args) {
        String filepath = "flights.txt";
        HashMap<String, Flight> flightList = new HashMap<String, Flight>();                     //hashmap list of flights 
        FlightPrinter fp = new FlightPrinter();                                                 //needed to print flight data
        FlightGraph flightNetwork = new FlightGraph(true, true);         //instantiate new flightgraph
        ArrayList<AirportVertex> airportVertices = new ArrayList<AirportVertex>();              //declare arraylist of airportvertices

        Airport[] airports = getAirports("top10usa.txt");                             //get list of airports from file
        for (Airport a : airports){
             airportVertices.add(flightNetwork.addVertex(a));                    //create new airportVertices from list of airports
        }
        
        FlightWriter.writeFlights(filepath, 50);                       //optional: generate and write flights to file
        flightList = FlightReader.readFlights(filepath);                                        //read flights from file

        HashMap<String, ArrayList<Flight>> flightIndex = FlightGenerator.flightMapper(flightList);  //generate new flightIndex hashmap

        // Create a HashMap to quickly look up vertices by airport code
        HashMap<String, AirportVertex> vertexMap = new HashMap<>();
        for (AirportVertex v : airportVertices) {
            vertexMap.put(v.getData().getCode(), v);
        }


        // Iterate through flightIndex and add edges
        for (ArrayList<Flight> flights : flightIndex.values()) {
            for (Flight f : flights) {
                AirportVertex origin = vertexMap.get(f.getOrigin().getCode());
                AirportVertex dest = vertexMap.get(f.getDestination().getCode());

                
                if (origin != null && dest != null) {
                    flightNetwork.addEdge(origin, dest, f.getPrice(), f.getFlightNumber());
                }
            }
        }
        flightNetwork.print();
        System.out.println("");
        menu(flightIndex, flightList, vertexMap);

        


        /* 
        //test code to check if flightIndex was created successfully
        ArrayList<Flight> temp = new ArrayList<>();
        temp = flightIndex.get("DFWMCO");               //search for flights from JFK to SEA

        for (Flight f: temp){
            fp.print(f);
        }
        */

    }

    public static void menu(HashMap<String, ArrayList<Flight>> flightIndex, HashMap<String, Flight> flightList, HashMap<String, AirportVertex> vertexMap){
        Scanner scanner = new Scanner(System.in);
        String choice = "";
        
        while (!choice.equals("0")) {
            System.out.println("\n===== FLIGHT SEARCH MENU =====");
            System.out.println("1. View Route Counter");
            System.out.println("2. Search Flight by Number");
            System.out.println("3. Search Flights by Route");
            System.out.println("4. Depth First Traversal");
            System.out.println("5. Breadth First Traversal");
            System.out.println("6. Dijkstra Search");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            System.out.flush();
            
            choice = scanner.nextLine();
            
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
                    ArrayList<AirportVertex> visitedVertices = new ArrayList<AirportVertex>();
                    System.out.print("Enter Origin Airport: ");
                    System.out.flush();
                    String origin = scanner.nextLine();
                    System.out.print("Enter Destination Airport: ");
                    System.out.flush();
                    String destination = scanner.nextLine();
                    
                    AirportVertex originVertex = vertexMap.get(origin);
                    AirportVertex destVertex = vertexMap.get(destination);
                    
                    if (originVertex != null && destVertex != null) {
                        GraphTraverser.depthFirstTraversal(originVertex, destVertex, visitedVertices, 0, "");
                    } else {
                        System.out.println("Invalid airport code(s).");
                    }
                    break;
                case "5":
                    FlightGenerator.flightNumSearch(flightList);
                    break;
                case "6":
                    FlightGenerator.flightRouteSearch(flightIndex);
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

    public static Airport[] getAirports(String filePath) {
        FileReader fr = new FileReader(filePath);
        return fr.getAirports();
    }
    
}
