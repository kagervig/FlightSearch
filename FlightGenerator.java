import java.time.LocalTime;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


public class FlightGenerator {

    public static void main(String[] args) {

        /* //testing code
        Airport JFK = new Airport("JFK","John F. Kennedy International Airport",40.6398,-73.7789,"EST");

        Airport ATL = new Airport("ATL","Hartsfield-Jackson Atlanta International Airport",33.6404,-84.4199,"EST");

        Airport SEA = new Airport("SEA","Seattle-Tacoma International Airport",47.4502,-122.3088,"PST");

        Flight jfkatl = new Flight(JFK, ATL, Haversine.calcDistance(JFK, ATL), generateRandomLocalTime());
        jfkatl.setFlightNumber(1122);

        Flight atlsea = new Flight(ATL, SEA, Haversine.calcDistance(ATL, SEA), generateRandomLocalTime());
        atlsea.setFlightNumber(998);

        Flight seaatl = new Flight(SEA, ATL, Haversine.calcDistance(SEA, ATL), generateRandomLocalTime());
        seaatl.setFlightNumber(77);

        Flight atljfk = new Flight(ATL, JFK, Haversine.calcDistance(ATL, JFK), generateRandomLocalTime());
        atljfk.setFlightNumber(1123);

        Flight seajfk = new Flight(SEA, JFK, Haversine.calcDistance(SEA, JFK), generateRandomLocalTime());
        seajfk.setFlightNumber(655);

        FlightPrinter fp = new FlightPrinter();
        fp.print(jfkatl);
        fp.print(atlsea);
        fp.print(seaatl);
        fp.print(atljfk);
        fp.print(seajfk);

        */


        //read list of airports from file
        FileReader fr = new FileReader("top10usa.txt");

        Airport[] airports = fr.getAirports();
        AirportPrinter ap = new AirportPrinter();

        //print all airports read
        // for (Airport airport : airports){
        //     ap.print(airport);
        // }

        int arraySize = airports.length;

        //randomly generate 2 ints 0 < arraySize
        //use ints to select from the array of airports
        //cannot be the same

        int one = 0;
        int two = 0;
        HashMap<String, Flight> flightList = new HashMap();

        /*one = (int) ((Math.random()) * arraySize);
        two = (int) ((Math.random()) * arraySize);

        ap.print(airports[one]);
        ap.print(airports[two]);
        int duplicateCount = 0;
        int zeroCount = 0;
        int arraySizeCount = 0;

        for (int i = 0; i < 100; i++){
            one = (int) ((Math.random()) * arraySize);
            two = (int) ((Math.random()) * arraySize);
            if (two == one){
                duplicateCount++;
            }
            if (one == 0 || two == 0){
                zeroCount++;
            }
        }

        System.out.println("Total duplicates: " + duplicateCount);
        System.out.println("Total zeroes: " + zeroCount);
        System.out.printf("Total %ds: %d", arraySize, zeroCount);
        */

        //generate 100 flights
        for (int i = 0; i < 100; i++){
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
            Flight newFlight = new Flight(origin, destination, Haversine.calcDistance(origin, destination), generateRandomLocalTime(), newFlightNum);
            flightList.put(newFlightNum, newFlight);    
            if (i % 10 == 0){
                System.out.printf("%d...", i);
            }
        }

        
        //iterate through HashMap and print flight numbers 
        for (String i : flightList.keySet()) {
            System.out.print(i + ", ");
        }

        //menu to print flight details
        FlightPrinter fp = new FlightPrinter();
        Scanner scnr = new Scanner(System.in);
        System.out.println("Enter a flight number: ");
        System.out.println("or 0 to quit");
        String choice = scnr.nextLine();
        
        while (!choice.equals("0")){
            fp.print(flightList.get(choice));
            System.out.println("Enter a flight number: ");
            choice = scnr.nextLine(); 
            fp.print(flightList.get(choice));
        }

        








        
        //read airport data from file

        //2 generate 100 flights, randomly pairing two different cities
            //generate flight number
                //check if flight number already in use
                //try binary search to find flight numbers? (requires array of flight numbers, dumb idea)
                //try dictionary to find flight numbers (use flightnum as key)
                //try hashmap to find flight numbers (unsure what this means, read up on it)
            //calculate distance between airports
            //calculate duration
            //generate random start time
            //calculate end time based on time zone

        /*display flights as:
        Flight Number:
        Origin:
        Destination:
        Departs:
        Arrives:
        Duration:
        */

    }

    public static LocalTime generateRandomLocalTime() {
    // 86400 is the number of seconds in a day
    int randomSecond = ThreadLocalRandom.current().nextInt(86400);
    return LocalTime.ofSecondOfDay(randomSecond);
    }


}
