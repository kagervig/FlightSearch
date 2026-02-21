package com.kristian.flightsearch.datagenerator;

import com.kristian.flightsearch.models.Airline;
import com.kristian.flightsearch.models.Airport;


public class NewFlightGenerator {



    public static Airline[] getAirlines(){
        return new Airline[0]; //fixme
    }

    public static Airport[] getAirports(String filePath){
        AirportFileReader fr = new AirportFileReader(filePath);                       //read list of airports from file
        Airport[] airports = fr.getAirports();                          //populates list of airports
        return airports;
    }


    
}
