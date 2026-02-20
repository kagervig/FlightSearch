package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.models.Flight;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FlightWriter {

    public static void main(String[] args) {
        //writeMonthOfFlights("monthofflights.txt", 500, "609airports.txt");
        writeFlights("flights.txt", 50000, "609airports.txt");
    }
    
    public static void writeFlights(String outputFilePath, int quantity, String airportFile) {
        // Get airports from file
        Airport[] airports = getAirports(airportFile);
        
        // Generate flights
        HashMap<String, Flight> flightList = generateFlights(quantity, airports);
        
        // Write to file
        writeFlightsToFile(outputFilePath, flightList);
        System.out.println("Wrote " + flightList.size() + " flights to " + outputFilePath);
    }
    
    public static void writeMonthOfFlights(String outputFilePath, int flightsPerDay, String airportFile) {
        Airport[] airports = getAirports(airportFile);
        LocalDate startDate = LocalDate.now();

        List<DatedFlight> allFlights = new ArrayList<>();

        for (int day = 0; day < 31; day++) {
            LocalDate flightDate = startDate.plusDays(day);
            HashMap<String, Flight> dailyFlights = generateFlights(flightsPerDay, airports);

            for (Flight flight : dailyFlights.values()) {
                allFlights.add(new DatedFlight(flight, flightDate));
            }
        }

        writeMonthFlightsToFile(outputFilePath, allFlights);
        System.out.println("Wrote " + allFlights.size() + " flights (31 days) to " + outputFilePath);
    }

    private static void writeMonthFlightsToFile(String filePath, List<DatedFlight> flights) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Date,FlightNumber,OriginCode,DestinationCode,Distance,DepartureTime,Price\n");

            for (DatedFlight df : flights) {
                Flight flight = df.flight;
                String line = String.format("%s,%s,%s,%s,%.2f,%s,%d\n",
                    df.date,
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

    private static class DatedFlight {
        final Flight flight;
        final LocalDate date;

        DatedFlight(Flight flight, LocalDate date) {
            this.flight = flight;
            this.date = date;
        }
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
        FSFileReader fr = new FSFileReader(filePath);
        return fr.getAirports();
    }
}
