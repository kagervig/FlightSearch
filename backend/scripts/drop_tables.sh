#!/bin/bash
# Drops all flight data tables. Run this to reset before re-seeding.
# Usage: ./drop_tables.sh <connection-string>

CONN="$1"

if [ -z "$CONN" ]; then
    echo "Usage: $0 <connection-string>"
    exit 1
fi

# Drop in reverse dependency order to respect foreign key constraints
psql "$CONN" << SQL
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS airlines;
DROP TABLE IF EXISTS airports;
DROP TABLE IF EXISTS planes;
DROP TABLE IF EXISTS airports_staging;
SQL

echo "Done."
