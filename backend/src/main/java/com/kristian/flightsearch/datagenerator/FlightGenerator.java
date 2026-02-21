package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.utils.FlightPrinter;
import com.kristian.flightsearch.utils.AirportPrinter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


public class FlightGenerator {

    public static void main(String[] args) {
        findMaxFlightLength();
        
        String filePath = "609airports.txt";
        Airport[] airports = getAirports(filePath);         //populates list of airports
        AirportPrinter ap = new AirportPrinter();           //instantiate a new airport printer
        FlightPrinter fp = new FlightPrinter();                
        
        //populate hashmap with flights
        HashMap<String, Flight> flightList = generateFlights(50000, airports);
        HashMap<String, ArrayList<Flight>> flightIndex = flightMapper(flightList);

        ArrayList<Flight> temp = new ArrayList<>();
        //temp = flightIndex.get("DFWMCO");               //search for flights from JFK to SEA

        // for (Flight f: temp){
        //     fp.print(f);
        // }

        routeCounter(flightIndex);
        //flightNumSearch(flightList);
        //flightRouteSearch(flightIndex);

    }


    public static void routeCounter(HashMap<String, ArrayList<Flight>> flightIndex){
        HashMap<String, Integer> routeCounter = new HashMap();
        System.out.println("There are " + flightIndex.size() + " total routes");
        // for (String route : flightIndex.keySet()){
        //     System.out.println(route + " has " + flightIndex.get(route).size() + " daily flights");
        //     //System.out.println(route);
            
        // }

    }

    public static HashMap<String, ArrayList<Flight>> flightMapper(HashMap<String, Flight> flightList){

        HashMap<String, ArrayList<Flight>> flightIndex = new HashMap();
        

        //creates a hashmap of flight route (E.G. JFKSEA)
        // value is arraylist of type Flight
        //iterate through flightList, for each flight:
            //concatenate origin & dest as key.
            //if origin-destination are the same, add to arraylist

        for (Flight f : flightList.values()){
            String route = f.getOrigin().getCode() + f.getDestination().getCode();
            //System.out.println(route);
            flightIndex.putIfAbsent(route, new ArrayList<>());
            flightIndex.get(route).add(f);
        }

        return flightIndex;
    }

    public static void printFlightNums(HashMap<String, Flight> flightList){
        //iterate through HashMap and print flight numbers 
        for (String i : flightList.keySet()) {
            System.out.print(i + ", ");
        }
    }

    public static LocalTime generateRandomLocalTime() {
    // 86400 is the number of seconds in a day
    int randomSecond = ThreadLocalRandom.current().nextInt(86400);
    return LocalTime.ofSecondOfDay(randomSecond);
    }

    public static void flightNumSearch(HashMap<String, Flight> flightList){
        //menu to print flight details
        FlightPrinter fp = new FlightPrinter();
        Scanner scnr = new Scanner(System.in);
        System.out.println("Enter a flight number: ");
        System.out.println("or 0 to quit");
        String choice = scnr.nextLine();

        while (!choice.equals("0")){
            fp.print(flightList.get(choice));
            System.out.println("Enter a flight number: ");
            choice = scnr.nextLine(); 
            if (choice.equals("0")){
                return;
            }
            fp.print(flightList.get(choice));
        }
        

    }
    public static ArrayList<Flight> flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex, String origin, String destination){
        ArrayList<Flight> temp = flightIndex.get(origin + destination);
        return temp;
    }

    public static void flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex){
        FlightPrinter fp = new FlightPrinter();
        Scanner scnr = new Scanner(System.in);
        System.out.println("(0 to quit)");
        System.out.println("Enter an origin airport: ");
        String origin = scnr.nextLine();
        if (origin.equals("0")){
            return;
        }
        System.out.println("Enter a destination airport: ");
        String destination = scnr.nextLine();

        while (!origin.equals("0") && !destination.equals("0")){
            origin.toUpperCase();
            destination.toUpperCase();
        
            ArrayList<Flight> temp = flightIndex.get(origin + destination);
            if (temp == null){
                System.out.println("No flights found, try another search.");
            }  else {
                for (Flight f: temp){
                    fp.print(f);
                }
            }

            System.out.println("Enter an origin airport: ");
            origin = scnr.nextLine();
            if (origin == "0"){
                return;
            }
            System.out.println("Enter a destination airport: ");
            destination = scnr.nextLine();

        }

    }

    public static HashMap<String, Flight> generateFlights(int quantity, Airport[] airports){
        //generate n flights using read airport data
        HashMap<String, Flight> flightList = new HashMap();
        int one, two;
        int arraySize = airports.length;                     //how many airports in the list


        //fill hashmap with random flights
        for (int i = 0; i < quantity; i++){
            one = (int) ((Math.random()) * arraySize - 1);
            two = (int) ((Math.random()) * arraySize - 1);
            while (two == one){
                two = (int) ((Math.random()) * arraySize - 1);
            }
            Airport origin = airports[one];
            Airport destination = airports[two];
            if (isFlightPossible(origin, destination)){
                String newFlightNum = Flight.generateFlightNum();
            if (flightList.get(newFlightNum) != null){
                newFlightNum = Flight.generateFlightNum();
            }
            Flight newFlight = new Flight(origin, destination, FlightDistanceCalculator.calcDistance(origin, destination), generateRandomLocalTime(), newFlightNum);
            flightList.put(newFlightNum, newFlight);  
            }              
        }
        return flightList;
    }

    public static boolean isFlightPossible(Airport origin, Airport destination) {

    double flightDistanceKm = FlightDistanceCalculator.calcDistance(origin, destination);
    double runwayLengthMeters = (double) origin.getRunwayLength();

    // 1. Constants for 1,000m ASL elevation adjustment (~23.3% increase)
    final double ALTITUDE_FACTOR = 1.233;
    
    // 2. Performance Baselines (Range in km, Runway in meters at Sea Level)
    final double MIN_RANGE = 0.0;
    final double MAX_RANGE = 16700.0; // A350-1000 Max Range
    
    // 3. Define Runway Requirements at MTOW (adjusted for 1,000m ASL)
    // A220-300 needs ~1,890m at Sea Level; at 1,000m it needs ~2,330m
    final double MIN_RUNWAY_REQUIRED = 1500 * ALTITUDE_FACTOR; // Short ferry flight
    final double MAX_RUNWAY_REQUIRED = 2900 * ALTITUDE_FACTOR; // Ultra-long haul
    
    // 4. Hard range limit (No commercial aircraft exceeds ~18,000km easily)
    if (flightDistanceKm > 18000) return false;
    if (flightDistanceKm < 100) return false;
    
    // 5. Model the required runway based on distance
    // We assume a linear-to-exponential growth for runway need vs range
    double requiredRunway;
    if (flightDistanceKm <= 6300) {
        // A220 class range
        double ratio = flightDistanceKm / 6300.0;
        requiredRunway = (1890 * ALTITUDE_FACTOR) * (0.8 + 0.2 * ratio);
    } else {
        // Widebody class range (up to 16,700km)
        double ratio = (flightDistanceKm - 6300) / (MAX_RANGE - 6300);
        double startRunway = 1890 * ALTITUDE_FACTOR;
        double endRunway = 3100 * ALTITUDE_FACTOR; // 787-9/A350 MTOW runway
        requiredRunway = startRunway + (ratio * (endRunway - startRunway));
    }
    
    return runwayLengthMeters >= requiredRunway;
}

    public static void findMaxFlightLength(){
        Airport[] airports = getAirports("609airports.txt");
        System.out.println("Total airports: " + airports.length);
        double maxFlightDistance = 0;
        String longestRoute = "No routes found";

        for (int i = 0; i < airports.length; i++){
            for (int j = 1; j < airports.length; j++){
                if (FlightDistanceCalculator.calcDistance(airports[i], airports[j]) > maxFlightDistance){
                    maxFlightDistance = FlightDistanceCalculator.calcDistance(airports[i], airports[j]);
                    longestRoute = (airports[i].getCode() + " to " + airports[j].getCode());
                }
            }
        }
        System.out.println("Longest route is: " + longestRoute);
        System.out.printf("Total Distance: %.0f\n", maxFlightDistance);
        
    }

    public static Airport[] getAirports(String filePath){
        AirportFileReader fr = new AirportFileReader(filePath);                       //read list of airports from file
        Airport[] airports = fr.getAirports();                          //populates list of airports
        return airports;
    }


}
