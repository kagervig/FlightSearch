This app will be a tool to search for the best route between multiple cities. The goal is to help travelers find an ideal route for a trip that will visit multiple cities, rather than searching for each leg independently. The tool will optimise for either lowest overall cost, shortest overall flight duration, or fewest layovers/stops.

# Airport
The airport class holds the airport details:
- String code
- String name
- double lat (latitude)
- double lon (longitude)
- String time zone

# AirportPrinter
This class prints airport data to the console

# FileReader
Takes a String containing file path.
Reads a .txt file with comma separated values in the order:
- airport code (E.G. JFK)
- airport name (E.G. John F. Kennedy International Airport)
- airport latitude (E.G. 40.6398)
- airport longitude (E.G. -73.7789)
- Time Zone (E.G. EST)

getAirports() method will populate an array of type Airport.

# Flight
The flight class holds:
- Airport origin
- Airport destination
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

# FlightDurationCalculator
estimates flight duration based on distance.

# FlightDistanceCalculator
Uses the haversine formula to calculate distnace in nautical miles between two geo-coordinates.

Takes as input two Airport objects, returns double distance.

# FlightGenerator
generateFlights() takes as input an int (number of flights to generate) and an array of type Airport. Will randomly generate n number of flights.

printFlightNums() takes in  a HashMap of flight numbers and Flight objects and prints only flight numbers in a comma separated list.

generateRandomLocalTime() returns a LocalTime object of a random departure time

getAirports() will call FileReader to read a file of airport data.

printFlightList() takes in a HashMap of flight numbers and Flight objects. It provides a menu where the user can input a flight number and it will print the flight details to the terminal.

# FlightPrinter
takes in a Flight object and prints the following:
- Flight number
- Origin airport
- Destination airport
- Departure time
- Arrival time
- Duration

