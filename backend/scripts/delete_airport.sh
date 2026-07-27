#!/bin/bash
# Deletes a single airport and all its flights from the database, then prints
# a verification query confirming both counts are 0. Intended for testing the
# airport removal process one airport at a time before running a bulk migration.
#
# Usage: ./delete_airport.sh <connection-string> <iata_code>

set -e

CONN="$1"
IATA=$(echo "${2}" | tr '[:lower:]' '[:upper:]')

if [ -z "$CONN" ] || [ -z "$IATA" ]; then
    echo "Usage: $0 <connection-string> <iata_code>"
    exit 1
fi

if ! [[ "$IATA" =~ ^[A-Z]{3}$ ]]; then
    echo "Error: IATA code must be exactly 3 letters (got: $IATA)"
    exit 1
fi

AIRPORT_NAME=$(psql "$CONN" -t -c "SELECT name FROM airports WHERE iata_code = '$IATA';" | xargs)
if [ -z "$AIRPORT_NAME" ]; then
    echo "Error: Airport $IATA not found in database"
    exit 1
fi

echo "Removing $IATA – $AIRPORT_NAME"

psql "$CONN" -v iata="$IATA" << 'SQL'
-- Delete all flights departing from or arriving at this airport
WITH deleted AS (
    DELETE FROM flights
    WHERE origin = :'iata' OR destination = :'iata'
    RETURNING flight_id
)
SELECT COUNT(*) AS flights_deleted FROM deleted;

-- Delete the airport row
DELETE FROM airports WHERE iata_code = :'iata'
RETURNING iata_code, name, city, country;

-- Verify both are gone (both values must be 0)
SELECT
    (SELECT COUNT(*) FROM airports WHERE iata_code = :'iata')                          AS airports_remaining,
    (SELECT COUNT(*) FROM flights  WHERE origin    = :'iata' OR destination = :'iata') AS flights_remaining;
SQL
