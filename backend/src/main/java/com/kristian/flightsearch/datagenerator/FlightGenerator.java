package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import com.kristian.flightsearch.utils.AirportPrinter;
import com.kristian.flightsearch.utils.FlightPrinter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


public class FlightGenerator {

    public static void main(String[] args) {
        String filePath = "top10usa.txt";
        Airport[] airports = getAirports(filePath);         //populates list of airports
        AirportPrinter ap = new AirportPrinter();           //instantiate a new airport printer
        FlightPrinter fp = new FlightPrinter();                
        
        //populate hashmap with flights
        HashMap<String, Flight> flightList = generateFlights(1000, airports);
        HashMap<String, ArrayList<Flight>> flightIndex = flightMapper(flightList);

        ArrayList<Flight> temp = new ArrayList<>();
        temp = flightIndex.get("DFWMCO");               //search for flights from JFK to SEA

        for (Flight f: temp){
            fp.print(f);
        }

        routeCounter(flightIndex);
        flightNumSearch(flightList);
        flightRouteSearch(flightIndex);


    }


    public static void routeCounter(HashMap<String, ArrayList<Flight>> flightIndex){
        HashMap<String, Integer> routeCounter = new HashMap();
        System.out.println("There are " + flightIndex.size() + " total routes");
        for (String route : flightIndex.keySet()){
            System.out.println(route + " has " + flightIndex.get(route).size() + " daily flights");
            //System.out.println(route);
            
        }

    }

    public static HashMap<String, ArrayList<Flight>> flightMapper(HashMap<String, Flight> flightList){

        HashMap<String, ArrayList<Flight>> flightIndex = new HashMap();
        

        //create hashmap of flight route (E.G. JFKSEA)
        //value is arraylist of type Flight
        //iterate through flightList, for each flight:
            //concat origin, dest as key.
            //if origin-destination are the same, add to arraylist

        for (Flight f : flightList.values()){
            String route = f.getOrigin().getCode() + f.getDestination().getCode();
            //System.out.println(route);
            flightIndex.putIfAbsent(route, new ArrayList<>());
            flightIndex.get(route).add(f);
        }

        //testing code
        /* 
        System.out.println("hashmap size: " + flightIndex.size());
        if (flightIndex.containsKey("JFKSEA")){
            System.out.println("JFKSEA exists");
        } else {
            System.out.println("JFKSEA not found");
        }
         if (flightIndex.containsKey("SEAJFK")){
            System.out.println("SEAJFK exists");
        } else {
            System.out.println("SEAJFK not found");
        }
            */
        
        

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
            if (choice == "0"){
                return;
            }
            fp.print(flightList.get(choice));
        }
        

    }

    public static void flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex){
        FlightPrinter fp = new FlightPrinter();
        Scanner scnr = new Scanner(System.in);
        System.out.println("or 0 to quit");
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

            String newFlightNum = Flight.generateFlightNum();
            if (flightList.get(newFlightNum) != null){
                newFlightNum = Flight.generateFlightNum();
            }
            Flight newFlight = new Flight(origin, destination, FlightDistanceCalculator.calcDistance(origin, destination), generateRandomLocalTime(), newFlightNum);
            flightList.put(newFlightNum, newFlight);    
        }
        return flightList;
    }

    public static Airport[] getAirports(String filePath){
        FileReader fr = new FileReader(filePath);                       //read list of airports from file
        Airport[] airports = fr.getAirports();                          //populates list of airports
        return airports;
    }


}
