# Flight Search App
This app will be a tool to search for the best route between multiple cities. The goal is to help travelers find an ideal route for a trip that will visit multiple cities, rather than searching for each leg independently. The tool will optimise for either lowest overall cost, shortest overall flight duration, or fewest layovers/stops.

Written with Java

# Models

### Airport
The airport class holds the airport details:
- String code
- String name
- double lat (latitude)
- double lon (longitude)
- String time zone

### Flight
The flight class holds:
- Airport airportOrigin
- Airport airportDestination
- Double distance
- LocalTime departureTime
- LocalTime arrivalTime 
- Duration duration 

There are two constructor methods:
1) will generate a flight number
requires:
- Airport origin
- Airport destination
- double distance
- LocalTime departureTime
2) accepts a flight number as input
requires:
- Airport origin
- Airport destination
- double distance
- LocalTime departureTime
- String flightNumber

generateFlightNum() will create a random flight number

# Flight Generation
This class generates dummy data for the purposes of building and testing the tool.

### FlightGenerator
generateFlights() takes as input an int (number of flights to generate) and an array of type Airport. Will randomly generate n number of flights.

printFlightNums() takes in  a HashMap of flight numbers and Flight objects and prints only flight numbers in a comma separated list.

generateRandomLocalTime() returns a LocalTime object of a random departure time

getAirports() will call FileReader to read a file of airport data.

printFlightList() takes in a HashMap of flight numbers and Flight objects. It provides a menu where the user can input a flight number and it will print the flight details to the terminal.


