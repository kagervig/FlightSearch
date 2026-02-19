# Method Summary

## Package: com.kristian.flightsearch

### Main
- `public static void main(String[] args)` — Loads airports and flights, builds the flight graph, and launches the interactive menu.
- `public static void menu(HashMap<String, ArrayList<Flight>> flightIndex, HashMap<String, Flight> flightList, FlightGraph flightNetwork, FSFileReader fileReader)` — Runs an interactive CLI menu for searching flights, traversals, and Dijkstra searches.
- `public static Airport[] getAirports(String filePath)` — Reads and returns all airports from the given file path.

### Server
- `public static void main(String[] args)` — Initialises flight data and starts the Javalin REST API server with CORS support.

---

## Package: com.kristian.flightsearch.models

### Airport
- `public Airport(String code, String name, double lat, double lon, String timeZone, int runwayLength, String city, String country)` — Constructs an Airport with all attributes.
- `public String getName()` — Returns the airport name.
- `public String getCode()` — Returns the 3-letter IATA code.
- `public double getLat()` — Returns the latitude.
- `public double getLon()` — Returns the longitude.
- `public String getTimeZone()` — Returns the timezone string.
- `public int getRunwayLength()` — Returns the runway length in metres.
- `public String getCity()` — Returns the city name.
- `public String getCountry()` — Returns the country name.

### Flight
- `public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime)` — Constructs a Flight with auto-generated flight number and price.
- `public Flight(Airport origin, Airport destination, double distance, LocalTime departureTime, String flightNumber)` — Constructs a Flight with a specified flight number.
- `public Airport getOrigin()` — Returns the origin airport.
- `public Integer getPrice()` — Returns the ticket price.
- `public Airport getDestination()` — Returns the destination airport.
- `public double getDistance()` — Returns the flight distance.
- `public String getFlightNumber()` — Returns the flight number string.
- `public LocalTime getDepartureTime()` — Returns the departure time.
- `public LocalTime getArrivalTime()` — Returns the calculated arrival time.
- `public Duration getDuration()` — Returns the flight duration.
- `public void setOrigin(Airport origin)` — Sets the origin airport.
- `public void setDestination(Airport destination)` — Sets the destination airport.
- `public void setPrice(int price)` — Sets the ticket price.
- `public void setFlightNumber(String flightNumber)` — Sets the flight number.
- `public void setDistance(int distance)` — Sets the flight distance.
- `public void setDepartureTime(LocalTime departureTime)` — Sets the departure time.
- `public void setArrivalTime(LocalTime arrivalTime)` — Sets the arrival time.
- `public static String generateFlightNum()` — Generates a random airline code + 4-digit flight number.
- `public int flightPricer(int distance)` — Calculates a semi-random ticket price based on distance.

---

## Package: com.kristian.flightsearch.datagenerator

### FSFileReader
- `public FSFileReader(String filePath)` — Constructs the reader and loads airports from the given CSV file.
- `public Airport[] getAirports()` — Returns all loaded airports as an array.
- `public int getAirportCount()` — Returns the number of airports loaded.
- `public Airport getAirportByCode(String code)` — Looks up an airport by its 3-letter code, returns null if not found.
- `public boolean isValidAirportCode(String code)` — Returns true if the airport code exists in the loaded data.
- `public static void main(String[] args)` — Demo entry point that loads and prints airports from a test file.

### FlightReader
- `public static HashMap<String, Flight> readFlights(String filePath, Airport[] airports)` — Reads flights from a CSV file and returns them keyed by flight number.

### FlightWriter
- `public static void main(String[] args)` — Demo entry point that generates a month of flights and writes to file.
- `public static void writeFlights(String outputFilePath, int quantity, String airportFile)` — Generates flights and writes them to a CSV file.
- `public static void writeMonthOfFlights(String outputFilePath, int flightsPerDay, String airportFile)` — Generates 31 days of flights and writes them with dates to a CSV file.
- `public static void writeFlightsToFile(String filePath, HashMap<String, Flight> flightList)` — Writes a HashMap of flights to a CSV file.
- `public static HashMap<String, Flight> generateFlights(int quantity, Airport[] airports)` — Generates a specified number of random flights between the given airports.
- `public static Airport[] getAirports(String filePath)` — Reads and returns airports from the given file.

### FlightGenerator
- `public static void main(String[] args)` — Demo entry point that generates flights, indexes them, and runs searches.
- `public static void routeCounter(HashMap<String, ArrayList<Flight>> flightIndex)` — Prints the total number of unique routes in the flight index.
- `public static HashMap<String, ArrayList<Flight>> flightMapper(HashMap<String, Flight> flightList)` — Groups flights by route key (origin+destination) into an indexed HashMap.
- `public static void printFlightNums(HashMap<String, Flight> flightList)` — Prints all flight numbers from the flight list.
- `public static LocalTime generateRandomLocalTime()` — Returns a random LocalTime within a 24-hour period.
- `public static void flightNumSearch(HashMap<String, Flight> flightList)` — Interactive CLI to search and print a flight by its flight number.
- `public static ArrayList<Flight> flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex, String origin, String destination)` — Returns flights between the given origin and destination codes.
- `public static void flightRouteSearch(HashMap<String, ArrayList<Flight>> flightIndex)` — Interactive CLI to search and print flights by origin/destination pair.
- `public static HashMap<String, Flight> generateFlights(int quantity, Airport[] airports)` — Generates random flights, filtering out those that fail the runway feasibility check.
- `public static boolean isFlightPossible(Airport origin, Airport destination)` — Returns whether a flight is feasible based on distance and runway length constraints.
- `public static void findMaxFlightLength()` — Finds and prints the longest possible route among all airport pairs.
- `public static Airport[] getAirports(String filePath)` — Reads and returns airports from the given file.

### FlightDistanceCalculator
- `public static double calcDistance(Airport airport1, Airport airport2)` — Calculates great-circle distance in km between two airports using the Haversine formula.
- `public static void main(String[] args)` — Demo entry point that calculates JFK-DEN distance.

### FlightDurationCalculator
- `public static Duration calculateFlightDuration(double distanceNM)` — Calculates flight duration from distance, including taxi/takeoff/landing overhead.

---

## Package: com.kristian.flightsearch.flightgraph

### FlightGraph
- `public FlightGraph(boolean isWeighted, boolean isDirected)` — Constructs a flight graph with the given weighted/directed settings.
- `public AirportVertex addVertex(Airport airport)` — Adds an airport as a vertex and indexes it by code.
- `public AirportVertex getVertex(String airportCode)` — Returns the vertex for the given airport code via O(1) HashMap lookup.
- `public void addEdge(AirportVertex vertex1, AirportVertex vertex2, Integer price, Duration duration, String flightNumber)` — Adds a directed edge (flight) between two airport vertices.
- `public void removeEdge(AirportVertex vertex1, AirportVertex vertex2)` — Removes the edge between two vertices.
- `public void removeVertex(AirportVertex vertex)` — Removes a vertex from the graph.
- `public ArrayList<AirportVertex> getVertices()` — Returns all vertices in the graph.
- `public boolean isWeighted()` — Returns whether the graph is weighted.
- `public boolean isDirected()` — Returns whether the graph is directed.
- `public void print()` — Prints all vertices and their edges to stdout.
- `public static void main(String[] args)` — Demo entry point that builds a small test graph and prints it.

### Graph
- `public Graph(boolean isWeighted, boolean isDirected)` — Constructs a graph with the given weighted/directed settings.
- `public AirportVertex addVertex(Airport data)` — Adds an airport as a vertex and returns it.
- `public void addEdge(AirportVertex vertex1, AirportVertex vertex2, Integer weight)` — Adds an edge with a weight; adds reverse edge if undirected.
- `public void removeEdge(AirportVertex vertex1, AirportVertex vertex2)` — Removes the edge between two vertices.
- `public void removeVertex(AirportVertex vertex)` — Removes a vertex from the graph.
- `public ArrayList<AirportVertex> getVertices()` — Returns all vertices.
- `public boolean isWeighted()` — Returns whether the graph is weighted.
- `public boolean isDirected()` — Returns whether the graph is directed.
- `public AirportVertex getVertexByCode(String code)` — Finds and returns a vertex by airport code via linear search.
- `public void print()` — Prints all vertices and their edges to stdout.

### NewFlightGraph
- `public NewFlightGraph(boolean isWeighted, boolean isDirected)` — Constructs a flight graph with HashMap-based vertex indexing.
- `public AirportVertex addVertex(Airport airport)` — Adds an airport as a vertex and indexes it by code.
- `public AirportVertex getVertex(String airportCode)` — Returns the vertex for the given airport code via O(1) lookup.
- `public void addEdge(AirportVertex vertex1, AirportVertex vertex2, Integer price, Duration duration, String flightNumber)` — Adds a directed edge between two airport vertices.
- `public void removeEdge(AirportVertex vertex1, AirportVertex vertex2)` — Removes the edge between two vertices.
- `public void removeVertex(AirportVertex vertex)` — Removes a vertex from the graph.
- `public ArrayList<AirportVertex> getVertices()` — Returns all vertices.
- `public boolean isWeighted()` — Returns whether the graph is weighted.
- `public boolean isDirected()` — Returns whether the graph is directed.
- `public void print()` — Prints all vertices and their edges to stdout.
- `public static void main(String[] args)` — Demo entry point that creates an empty graph and prints it.

### AirportVertex
- `public AirportVertex(Airport inputData)` — Constructs a vertex wrapping the given Airport.
- `public void addEdge(AirportVertex endVertex, Integer weight, Duration duration, String flightNumber)` — Adds an outgoing edge to another vertex.
- `public void removeEdge(AirportVertex endVertex)` — Removes all edges pointing to the given vertex.
- `public Airport getData()` — Returns the wrapped Airport object.
- `public ArrayList<Edge> getEdges()` — Returns all outgoing edges.
- `public void print(boolean showWeight)` — Prints this vertex and all its edges, optionally showing price and duration.

### Edge
- `public Edge(AirportVertex start, AirportVertex end, Integer price, Duration duration, String flightNumber)` — Constructs an edge with start/end vertices, price, duration, and flight number.
- `public AirportVertex getStart()` — Returns the starting vertex.
- `public AirportVertex getEnd()` — Returns the ending vertex.
- `public Integer getPrice()` — Returns the edge price (weight).
- `public Duration getDuration()` — Returns the flight duration.
- `public String getFlightNum()` — Returns the flight number.

### Dijkstra
- `public static Map[] searchByPrice(FlightGraph g, AirportVertex startingVertex)` — Runs Dijkstra's algorithm to find cheapest prices from the starting vertex to all others.
- `public static Map[] searchByDuration(FlightGraph g, AirportVertex startingVertex)` — Runs Dijkstra's algorithm to find shortest durations from the starting vertex to all others.
- `public static void printSearchResult(Map[] results)` — Prints the distance and previous-vertex maps from a Dijkstra search result.

### GraphTraverser
- `public static void depthFirstTraversal(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices)` — Performs a basic DFS from origin toward destination, printing "Success" if found.
- `public static void depthFirstTraversal(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices, int legs, String message, Duration totalDuration)` — Performs DFS with a max of 5 legs, printing each possible route found.
- `public static void breadthFirstSearch(AirportVertex origin, AirportVertex destination, ArrayList<AirportVertex> visitedVertices)` — Performs BFS from origin toward destination, printing visited vertices and leg counts.

### LinkedList
- `public void addToTail(AirportVertex data)` — Appends a node to the end of the list.
- `public AirportVertex removeHead()` — Removes and returns the data from the head node.

### Queue
- `public Queue()` — Constructs an empty queue backed by a LinkedList.
- `public boolean isEmpty()` — Returns true if the queue has no elements.
- `public void enqueue(AirportVertex data)` — Adds a vertex to the back of the queue.
- `public AirportVertex peek()` — Returns the front element without removing it, or null if empty.
- `public AirportVertex dequeue()` — Removes and returns the front element; throws Error if empty.

### QueueObject
- `public QueueObject(AirportVertex v, Integer p)` — Constructs a queue object pairing a vertex with a priority value.
- `public int compareTo(QueueObject o)` — Compares by priority for use in PriorityQueue ordering.

---

## Package: com.kristian.flightsearch.utils

### FlightPrinter
- `public void flightPrinter(Flight f)` — Sets the internal flight reference.
- `public void print(Flight flight)` — Prints formatted flight details (number, route, times, duration, price) to stdout.

### AirportPrinter
- `public void airportPrinter(Airport airport)` — Sets the internal airport reference.
- `public void print(Airport airport)` — Prints formatted airport details (code, name, timezone) to stdout.
- `public static void main(String[] args)` — Demo entry point that prints a test airport.

---

## Package: com.kristian.flightsearch.flightsearch

### flightSearch
- `public static void main(String[] args)` — Interactive CLI that takes a home airport and cities to visit, generates route permutations, filters by feasibility, and prices them.
- `public static void printRoutes(ArrayList<String[]> flightCombinations)` — Prints all route permutations to stdout.
- `public static void printRoutesWithPrice(ArrayList<String[]> flightCombinations, int[] totalRoutePrice)` — Prints all route permutations with their total prices.
- `public static ArrayList<String[]> flightCombinations(String[] airportsToVisit, String homeAirport)` — Generates all permutations of cities to visit, bookended by the home airport.
- `public static boolean hasFlightsForAllLegs(String[] flightRoute, HashMap<String, ArrayList<Flight>> flightIndex)` — Returns true if every consecutive leg in the route has available flights.
- `public static String[] captureAirports(int citiesToVisit, String homeAirport)` — Interactively prompts the user to enter validated airport codes.
