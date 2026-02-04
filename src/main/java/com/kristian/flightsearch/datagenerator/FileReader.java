package com.kristian.flightsearch.datagenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import com.kristian.flightsearch.utils.AirportPrinter;
import com.kristian.flightsearch.models.Airport;

public class FileReader {
    private Airport[] airports;

    public FileReader(String filePath) {
        readAirportsFromFile(filePath);
    }

    private void readAirportsFromFile(String filePath) {
        ArrayList<Airport> airportList = new ArrayList<>();
        
        try {
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);
            
            // Read line by line
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue; // Skip empty lines
                
                String[] airportData = line.split(",");
                
                // Process each airport entry (5 fields per airport)
                if (airportData.length >= 5) {
                    String code = airportData[0];
                    String name = airportData[1];
                    double lat = Double.parseDouble(airportData[2]);
                    double lon = Double.parseDouble(airportData[3]);
                    String timeZone = airportData[4];
                    
                    Airport airport = new Airport(code, name, lat, lon, timeZone);
                    airportList.add(airport);
                }
            }
            
            scanner.close();
            airports = airportList.toArray(new Airport[0]);
            
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
            airports = new Airport[0];
        }
    }

    public Airport[] getAirports() {
        return airports;
    }

    public int getAirportCount() {
        return airports.length;
    }
    public static void main(String[] args) {
        FileReader fr = new FileReader("top10usa.txt");

        Airport[] airports = fr.getAirports();
        AirportPrinter ap = new AirportPrinter();

        for (Airport airport : airports){
            ap.print(airport);
        }
    }
}
