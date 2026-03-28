#!/bin/bash
# One-off script to create schema and load CSV data into the database.
# Usage: ./seed_database.sh <connection-string>
#
# airports and flights are loaded via staging tables:
#   - airports: the CSV contains 'type' and 'source' columns not in the schema
#   - flights: filters out records with aircraft_type values missing from planes

set -e

CONN="$1"
DATA_DIR="$(cd "$(dirname "$0")/../data" && pwd)"

if [ -z "$CONN" ]; then
    echo "Usage: $0 <connection-string>"
    exit 1
fi

echo "Creating schema..."
psql "$CONN" -f "$DATA_DIR/schema.sql"

echo "Loading planes..."
psql "$CONN" -c "\copy planes (name, iata_code) FROM '$DATA_DIR/planes.csv' CSV NULL AS ''"

echo "Loading airports..."
psql "$CONN" << SQL
CREATE TABLE airports_staging (
    airport_id           INTEGER,
    name                 TEXT,
    city                 TEXT,
    country              TEXT,
    iata_code            CHAR(3),
    icao_code            CHAR(4),
    latitude             NUMERIC(9,6),
    longitude            NUMERIC(9,6),
    utc_offset           NUMERIC(4,1),
    dst                  CHAR(1),
    timezone             TEXT,
    type                 TEXT,
    source               TEXT,
    elevation_ft         INTEGER,
    max_runway_length_ft INTEGER
);
\copy airports_staging FROM '$DATA_DIR/airports.csv' CSV HEADER NULL AS ''
INSERT INTO airports (airport_id, iata_code, icao_code, name, city, country,
                      latitude, longitude, utc_offset, dst, timezone,
                      elevation_ft, max_runway_length_ft)
    SELECT airport_id, iata_code, icao_code, name, city, country,
           latitude, longitude, utc_offset, dst, timezone,
           elevation_ft, max_runway_length_ft
    FROM airports_staging;
DROP TABLE airports_staging;
SQL

echo "Loading airlines..."
psql "$CONN" -c "\copy airlines (airline_code, airline_name, country) FROM '$DATA_DIR/airlines.csv' CSV HEADER NULL AS ''"

echo "Loading flights..."
psql "$CONN" << SQL
CREATE TABLE flights_staging (
    flight_date    DATE,
    airline_code   VARCHAR(3),
    origin         CHAR(3),
    destination    CHAR(3),
    stops          SMALLINT,
    aircraft_type  VARCHAR(4),
    flight_number  VARCHAR(8),
    departure_time TIME,
    ticket_price   NUMERIC(8,2)
);
\copy flights_staging FROM '$DATA_DIR/flights.csv' CSV HEADER NULL AS ''
INSERT INTO flights (flight_date, airline_code, origin, destination, stops,
                     aircraft_type, flight_number, departure_time, ticket_price)
    SELECT flight_date, airline_code, origin, destination, stops,
           aircraft_type, flight_number, departure_time, ticket_price
    FROM flights_staging
    WHERE airline_code   IN (SELECT airline_code FROM airlines)
      AND origin         IN (SELECT iata_code FROM airports)
      AND destination    IN (SELECT iata_code FROM airports)
      AND (aircraft_type IS NULL OR aircraft_type IN (SELECT iata_code FROM planes));
DROP TABLE flights_staging;
SQL

echo "Done."
