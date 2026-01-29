import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


public class FlightGenerator {

    public static void main(String[] args) {
        String filePath = "top10usa.txt";
        Airport[] airports = getAirports(filePath);                     //populates list of airports
        AirportPrinter ap = new AirportPrinter();                       //instantiate a new airport printer
        
        //populate hashmap with flights
        HashMap<String, Flight> flightList = generateFlights(100, airports);

    }

    public HashMap<String, ArrayList<Flight>> flightMapper(HashMap<String, Flight> flightList){
        HashMap<String, ArrayList<Flight>> flightIndex = new HashMap();

        //iterate through flightList, for each flight:
            //concat origin, dest as key.
            //if origin-destination are the same, add to arraylist

       


        return flightIndex;
    }

    public static void printFlightNums(HashMap<String, Flight> flightList){
        //iterate through HashMap and print flight numbers 
        for (String i : flightList.keySet()) {
            System.out.print(i + ", ");
        }
    }

    public static LocalTime generateRandomLocalTime() {
    // 86400 is the number of seconds in a day
    int randomSecond = ThreadLocalRandom.current().nextInt(86400);
    return LocalTime.ofSecondOfDay(randomSecond);
    }

    public static void printFlightList(HashMap<String, Flight> flightList){
        //menu to print flight details
        FlightPrinter fp = new FlightPrinter();
        Scanner scnr = new Scanner(System.in);
        System.out.println("Enter a flight number: ");
        System.out.println("or 0 to quit");
        String choice = scnr.nextLine();
        fp.print(flightList.get(choice));

        while (!choice.equals("0")){
            System.out.println("Enter a flight number: ");
            choice = scnr.nextLine(); 
            fp.print(flightList.get(choice));
        }

    }

    public static HashMap<String, Flight> generateFlights(int quantity, Airport[] airports){
        //generate n flights using read airport data
        HashMap<String, Flight> flightList = new HashMap();
        int one, two;
        int arraySize = airports.length;                     //how many airports in the list


        //fill hashmap with random flights
        for (int i = 0; i < quantity; i++){
            one = (int) ((Math.random()) * arraySize - 1);
            two = (int) ((Math.random()) * arraySize - 1);
            if (two == one && two < arraySize - 1){
                two++;
            } else if (two == one && two == 0){
                two++;
            } else {
                two++;
            }
            Airport origin = airports[one];
            Airport destination = airports[two];

            String newFlightNum = Flight.generateFlightNum();
            if (flightList.get(newFlightNum) != null){
                newFlightNum = Flight.generateFlightNum();
            }
            Flight newFlight = new Flight(origin, destination, FlightDistanceCalculator.calcDistance(origin, destination), generateRandomLocalTime(), newFlightNum);
            flightList.put(newFlightNum, newFlight);    
        }
        return flightList;
    }

    public static Airport[] getAirports(String filePath){
        FileReader fr = new FileReader(filePath);                       //read list of airports from file
        Airport[] airports = fr.getAirports();                          //populates list of airports
        return airports;
    }


}
