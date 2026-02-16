package com.kristian.flightsearch.datagenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;

public class FlightReader {

    
    
    
    public static HashMap<String, Flight> readFlights(String filePath) {
        return readFlights(filePath, "top100global.txt");
    }

    public static HashMap<String, Flight> readFlights(String filePath, String airportFile) {
        HashMap<String, Flight> flightList = new HashMap<>();
        Airport[] airports = getAirports(airportFile);
        
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                // Skip header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 6) {
                    String flightNumber = parts[0];
                    String originCode = parts[1];
                    String destCode = parts[2];
                    double distance = Double.parseDouble(parts[3]);
                    LocalTime departureTime = LocalTime.parse(parts[4]);
                    int price = Integer.parseInt(parts[5]);
                    
                    // Find airports by code
                    Airport origin = findAirportByCode(airports, originCode);
                    Airport destination = findAirportByCode(airports, destCode);
                    
                    if (origin != null && destination != null) {
                        Flight flight = new Flight(origin, destination, distance, departureTime, flightNumber);
                        flight.setPrice(price);
                        flightList.put(flightNumber, flight);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading flights from file: " + e.getMessage());
        }
        
        return flightList;
    }
    
    private static Airport findAirportByCode(Airport[] airports, String code) {
        for (Airport airport : airports) {
            if (airport.getCode().equals(code)) {
                return airport;
            }
        }
        return null;
    }
    
    private static Airport[] getAirports(String filePath) {
        FileReader fr = new FileReader(filePath);
        Airport[] airports = fr.getAirports();                          //populates list of airports
        return airports;
    }
}
