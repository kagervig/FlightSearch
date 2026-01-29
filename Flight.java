import java.time.Duration;
import java.time.LocalTime;
import java.util.Random;


public class Flight {
    Airport origin;
    Airport destination;
    double distance;
    LocalTime departureTime, arrivalTime;
    String flightNumber; 
    Duration duration;
    static String[] airlineCodes = {"AA","UA","DL","WN","B6","AS","NK","F9","G4","SY"};

    public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime){
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.departureTime = departureTime;
        this.duration = FlightDurationCalculator.calculateFlightDuration(distance);
        this.arrivalTime = this.departureTime.plus(this.duration);
        this.flightNumber = generateFlightNum();
    }
    public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime, String flightNumber){
        this.origin = origin;
        this.destination = destination;
        this.distance = distance;
        this.departureTime = departureTime;
        this.duration = FlightDurationCalculator.calculateFlightDuration(distance);
        this.arrivalTime = this.departureTime.plus(this.duration);
        this.flightNumber = flightNumber;
    }

    // Getters
    public Airport getOrigin() {
        return origin;
    }

    public Airport getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }
    public String getFlightNumber() {
        return flightNumber;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }
    public Duration getDuration() {
        return duration;
    }

    // Setters
    public void setOrigin(Airport origin) {
        this.origin = origin;
    }

    public void setDestination(Airport destination) {
        this.destination = destination;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setdepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }
    public void setdArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    public static String generateFlightNum(){
        Random random = new Random();
        int flightNum = random.nextInt(9999) + 1;
        int randomAirline = random.nextInt(airlineCodes.length);
        String airlineCode = airlineCodes[randomAirline];
        return airlineCode + String.format(" %04d", flightNum);
    }
}

