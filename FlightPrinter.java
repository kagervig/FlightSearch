import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class FlightPrinter {
    
    Flight flight;

    public void flightPrinter(Flight f){
        this.flight = f;
    }

    
    public void print(Flight flight) {
        // Add logic to print the flight details
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm");

        System.out.println("Flight Number: " + flight.getFlightNumber());
        System.out.println("Origin: " + flight.origin.getCode());
        System.out.println("Destination: " + flight.destination.getCode());
        String formattedTime = flight.getDepartureTime().format(myFormatObj);
        System.out.println("Departs: " + formattedTime);
        formattedTime = flight.getArrivalTime().format(myFormatObj);
        System.out.println("Arrives: " + formattedTime);
        Duration d = flight.getDuration();
        formattedTime = String.format("%2d Hours %02d Minutes",d.toHours(), d.toMinutesPart());
        System.out.println("Duration: " + formattedTime);
        System.out.println("");
    }
}
