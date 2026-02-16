package com.kristian.flightsearch.datagenerator;

/*
 * FileReader.java - Loads airport data from a text file
 *
 * This class reads airport information from a CSV-formatted text file and provides
 * methods to look up airports by their code.
 *
 * File format (top100global.txt):
 *   CODE,Name,Latitude,Longitude,Timezone,RunwayLength
 *   JFK,John F. Kennedy International Airport,40.6413,-73.7781,America/New_York,4423
 *   LAX,Los Angeles International Airport,33.9416,-118.4085,America/Los_Angeles,3939
 *
 * Key features:
 *   - Reads from JAR classpath (for deployment) or filesystem (for local dev)
 *   - Provides O(1) lookup of airports by code using a HashMap
 *   - Validates airport codes
 */

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.utils.AirportPrinter;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class FileReader {
    private Airport[] airports;           // Array of all airports (for iteration)
    private HashMap<String, Airport> airportMap;  // Map for O(1) lookup by code

    public FileReader(String filePath) {
        readAirportsFromFile(filePath);
    }

    private void readAirportsFromFile(String filePath) {
        ArrayList<Airport> airportList = new ArrayList<>();
        airportMap = new HashMap<>();

        try {
            // Get a reader for the file (tries classpath first, then filesystem)
            BufferedReader reader = getReader(filePath);
            if (reader == null) {
                System.out.println("File not found: " + filePath);
                airports = new Airport[0];
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Parse CSV: CODE,Name,Lat,Lon,Timezone,RunwayLength
                String[] airportData = line.split(",");

                if (airportData.length >= 6) {
                    String code = airportData[0];
                    String name = airportData[1];
                    double lat = Double.parseDouble(airportData[2]);
                    double lon = Double.parseDouble(airportData[3]);
                    String timeZone = airportData[4];
                    int runwayLength = Integer.parseInt(airportData[5]);

                    Airport airport = new Airport(code, name, lat, lon, timeZone, runwayLength);
                    airportList.add(airport);
                    airportMap.put(code, airport);  // Index by code for fast lookup
                }
            }

            reader.close();
            airports = airportList.toArray(new Airport[0]);

        } catch (Exception e) {
            System.out.println("Error reading file: " + filePath + " - " + e.getMessage());
            airports = new Airport[0];
            airportMap = new HashMap<>();
        }
    }

    /**
     * Gets a reader for the file - tries classpath first (for JAR), then filesystem (for local dev)
     *
     * Why two approaches?
     * - When deployed to Railway, files are bundled INSIDE the JAR file
     *   We access them using getResourceAsStream() which reads from the classpath
     * - When developing locally, files exist on the filesystem
     *   We use regular file reading
     *
     * By trying classpath first, deployment works. By falling back to filesystem,
     * local development works too.
     */
    private BufferedReader getReader(String filePath) {
        // Try classpath first (works when running from JAR)
        // getResourceAsStream looks inside the JAR for files in src/main/resources
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        if (is != null) {
            return new BufferedReader(new InputStreamReader(is));
        }

        // Fall back to filesystem (works for local development)
        File file = new File(filePath);
        if (file.exists()) {
            try {
                return new BufferedReader(new java.io.FileReader(file));
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    // Returns all airports as an array
    public Airport[] getAirports() {
        return airports;
    }

    // Returns the number of airports loaded
    public int getAirportCount() {
        return airports.length;
    }

    // Look up an airport by its 3-letter code (e.g., "JFK")
    // Returns null if not found
    public Airport getAirportByCode(String code) {
        return airportMap.get(code.toUpperCase());
    }

    // Check if an airport code exists in our data
    // Used for validating user input
    public boolean isValidAirportCode(String code) {
        return airportMap.containsKey(code.toUpperCase());
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
