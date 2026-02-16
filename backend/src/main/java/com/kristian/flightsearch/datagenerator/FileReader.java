package com.kristian.flightsearch.datagenerator;

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
    private Airport[] airports;
    private HashMap<String, Airport> airportMap;

    public FileReader(String filePath) {
        readAirportsFromFile(filePath);
    }

    private void readAirportsFromFile(String filePath) {
        ArrayList<Airport> airportList = new ArrayList<>();
        airportMap = new HashMap<>();

        try {
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
                    airportMap.put(code, airport);
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
     */
    private BufferedReader getReader(String filePath) {
        // Try classpath first (works when running from JAR)
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

    public Airport[] getAirports() {
        return airports;
    }

    public int getAirportCount() {
        return airports.length;
    }
    public Airport getAirportByCode(String code) {
        return airportMap.get(code.toUpperCase());
    }

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
