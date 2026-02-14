package com.kristian.flightsearch.datagenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class FlightWriter {
    
    public static void main(String[] args) {
        writeFlights("flights.txt", 1000);
    }
    
    public static void writeFlights(String outputFilePath, int quantity) {
        // Get airports from file
        String airportFile = "top10usa.txt";
        Airport[] airports = getAirports(airportFile);
        
        // Generate flights
        HashMap<String, Flight> flightList = generateFlights(quantity, airports);
        
        // Write to file
        writeFlightsToFile(outputFilePath, flightList);
        System.out.println("Wrote " + flightList.size() + " flights to " + outputFilePath);
    }
    
    public static void writeFlightsToFile(String filePath, HashMap<String, Flight> flightList) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write("FlightNumber,OriginCode,DestinationCode,Distance,DepartureTime,Price\n");
            
            // Write each flight
            for (Flight flight : flightList.values()) {
                String line = String.format("%s,%s,%s,%.2f,%s,%d\n",
                    flight.getFlightNumber(),
                    flight.getOrigin().getCode(),
                    flight.getDestination().getCode(),
                    flight.getDistance(),
                    flight.getDepartureTime(),
                    flight.getPrice()
                );
                writer.write(line);
            }
        } catch (IOException e) {
            System.err.println("Error writing flights to file: " + e.getMessage());
        }
    }
    
    public static HashMap<String, Flight> generateFlights(int quantity, Airport[] airports) {
        HashMap<String, Flight> flightList = new HashMap();
        int one, two;
        int arraySize = airports.length;

        for (int i = 0; i < quantity; i++) {
            one = (int) ((Math.random()) * arraySize - 1);
            two = (int) ((Math.random()) * arraySize - 1);
            while (two == one) {
                two = (int) ((Math.random()) * arraySize - 1);
            }
            Airport origin = airports[one];
            Airport destination = airports[two];

            String newFlightNum = Flight.generateFlightNum();
            if (flightList.get(newFlightNum) != null) {
                newFlightNum = Flight.generateFlightNum();
            }
            Flight newFlight = new Flight(origin, destination, 
                FlightDistanceCalculator.calcDistance(origin, destination), 
                FlightGenerator.generateRandomLocalTime(), newFlightNum);
            flightList.put(newFlightNum, newFlight);
        }
        return flightList;
    }
    
    public static Airport[] getAirports(String filePath) {
        FileReader fr = new FileReader(filePath);
        return fr.getAirports();
    }
}
