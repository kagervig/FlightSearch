package com.kristian.flightsearch.utils;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

import com.kristian.flightsearch.models.Flight;

public class FlightPrinter {

    Flight flight;

    public void flightPrinter(Flight f) {
        this.flight = f;
    }

    public void print(Flight flight) {
        if (flight == null) {
            System.out.println("Error, flight not found");
            return;
        }

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        Duration d = flight.getDuration();
        String duration = String.format("%dh %02dm", d.toHours(), d.toMinutesPart());

        System.out.println("-".repeat(45));
        System.out.printf("%-12s %s → %s%n",
                flight.getFlightNumber(),
                flight.getOrigin().getCode(),
                flight.getDestination().getCode());
        System.out.printf("%-12s %s → %s%n",
                "",
                flight.getOrigin().getCity(),
                flight.getDestination().getCity());
        System.out.printf("%-12s %s → %s  (%s)%n",
                "",
                flight.getDepartureTime().format(timeFmt),
                flight.getArrivalTime().format(timeFmt),
                duration);
        System.out.printf("%-12s $%d%n", "", flight.getPrice());
    }
}
