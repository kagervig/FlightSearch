package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airport;
import com.kristian.flightsearch.utils.AirportPrinter;
import java.io.File;
import java.io.FileNotFoundException;
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
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
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

            scanner.close();
            airports = airportList.toArray(new Airport[0]);

        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filePath);
            airports = new Airport[0];
            airportMap = new HashMap<>();
        }
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
