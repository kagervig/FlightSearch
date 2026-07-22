#!/usr/bin/env python3
"""
Filter a flights CSV to only include flights between supported airports.

Usage:
    python3 filter_flights.py <airports.csv> <flights.csv> <output.csv>

The airports CSV must have an 'iata_code' column.
The flights CSV must have 'origin' and 'destination' columns.
"""

import csv
import sys


def main():
    if len(sys.argv) != 4:
        print("Usage: filter_flights.py <airports.csv> <flights.csv> <output.csv>")
        sys.exit(1)

    airports_path, flights_path, output_path = sys.argv[1:]

    supported = set()
    with open(airports_path, newline="") as f:
        for row in csv.DictReader(f):
            code = row["iata_code"].strip()
            if code:
                supported.add(code)

    print(f"Supported airports: {len(supported)}")

    kept = 0
    removed = 0
    unsupported_codes = set()

    with open(flights_path, newline="") as fin, open(output_path, "w", newline="") as fout:
        reader = csv.DictReader(fin)
        writer = csv.DictWriter(fout, fieldnames=reader.fieldnames)
        writer.writeheader()

        for row in reader:
            origin = row["origin"].strip()
            destination = row["destination"].strip()
            if origin in supported and destination in supported:
                writer.writerow(row)
                kept += 1
            else:
                if origin not in supported:
                    unsupported_codes.add(origin)
                if destination not in supported:
                    unsupported_codes.add(destination)
                removed += 1

    print(f"Flights kept:    {kept:,}")
    print(f"Flights removed: {removed:,}")
    print(f"Output: {output_path}")

    if unsupported_codes:
        print(f"\nAirport codes that caused removals ({len(unsupported_codes)}):")
        for code in sorted(unsupported_codes):
            print(f"  {code}")


if __name__ == "__main__":
    main()
