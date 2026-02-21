# Flight Search App
This app will be a tool to search for the best route between multiple cities. The goal is to help travelers find an ideal route for a trip that will visit multiple cities, rather than searching for each leg independently. The tool will optimise for either lowest overall cost, shortest overall flight duration, or fewest layovers/stops.

Written with Java

## Development

### Running the Backend Locally

```bash
cd backend
mvn compile exec:java
```

The server runs on http://localhost:8080

### Running with Debugger

```bash
cd backend
mvn compile exec:exec -Pdebug
```

This starts the server with a debug port on 5005. Then attach VS Code:
1. Open Run & Debug (Cmd+Shift+D)
2. Select "Attach to Debug Server"
3. Press F5

### Running Tests

```bash
cd backend
mvn test
```

### Building for Deployment

```bash
cd backend
mvn clean package -DskipTests
```

Creates a fat JAR at `target/flightsearch-1.0-SNAPSHOT.jar`

### Running the Frontend Locally

```bash
cd frontend
npm run dev
```

The frontend runs on http://localhost:3000. When running locally it points to the local backend at http://localhost:8080 via `frontend/.env.development.local`. The production Railway URL is in `frontend/.env.local` and is used for production builds.

### Database

#### Prerequisites

Install PostgreSQL 17:
```bash
brew install postgresql@17
brew services start postgresql@17
```

Add `psql` to your PATH (add this to `~/.zshrc` to make it permanent):
```bash
export PATH="/opt/homebrew/opt/postgresql@17/bin:$PATH"
```

#### Local Setup

Create the database:
```bash
psql postgres -c "CREATE DATABASE flightsearch;"
```

Run the migration:
```bash
psql flightsearch -f backend/src/main/resources/db/001_create_flights.sql
```

#### Schema

The `flights` table:

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key, auto-generated |
| scheduled_departure | TIMESTAMP | |
| scheduled_arrival | TIMESTAMP | Next day if arrival < departure |
| flight_number | VARCHAR | |
| origin | VARCHAR | Airport code |
| destination | VARCHAR | Airport code |
| price | INTEGER | |
| currency | VARCHAR | Defaults to USD |
| duration | INTEGER | Minutes |

Migrations are in `backend/src/main/resources/db/` and are numbered sequentially (e.g. `001_create_flights.sql`).

#### Seeding

On startup, the server automatically seeds the flights table from `flights.txt` if the table is empty. This runs on both local and Railway deployments — no manual steps required.

#### Production (Railway)

Set the `DATABASE_URL` environment variable in Railway. The server reads this on startup and connects automatically. The format Railway provides (`postgresql://...`) is handled automatically.

#### Querying the Local Database

```bash
psql flightsearch -c "SELECT * FROM flights LIMIT 10;"
```

### API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /health` | Health check |
| `GET /api/airports` | List all airports |
| `GET /api/flights/search?from=JFK&to=LAX` | Search direct flights |
| `GET /api/routes/cheapest?from=JFK` | Find cheapest routes using Dijkstra |

# Models

### Airport
The airport class holds the airport details:
- String code
- String name
- double lat (latitude)
- double lon (longitude)
- String time zone
- int runwayLength
- String city
- String country

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


