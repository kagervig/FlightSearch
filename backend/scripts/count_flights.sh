#!/bin/bash
# Queries the number of flights in the database.
# Usage: ./count_flights.sh <connection-string>

CONN="$1"

if [ -z "$CONN" ]; then
    echo "Usage: $0 <connection-string>"
    exit 1
fi

psql "$CONN" -c "SELECT COUNT(*) AS flight_count FROM flights;"
