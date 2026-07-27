#!/usr/bin/env python3
"""
Extract flights from europe_air_routes.csv and produce a CSV compatible
with the flights table schema.

For each route that is active on the target date:
  - One row per carrier serving the route
  - One row per flight that day (upper bound of flights_per_day range)
  - Departure times spread evenly between 06:00 and 22:00
  - Flight numbers derived from carrier + route id

Usage:
    python3 extract_europe_flights.py \\
        --routes <europe_air_routes.csv> \\
        --airports <airports.csv> \\
        --output <output.csv> \\
        [--date YYYY-MM-DD]   # default: 2026-07-14
        [--limit N]           # process only first N input routes (for testing)
"""

import argparse
import ast
import csv
import re
import sys
from datetime import date, datetime


# Codes used by the routes source that differ from IATA standard.
# All entries here now exist in the planes table, so no remapping is needed.
# This dict is kept as a no-op placeholder in case future source data
# introduces codes that require translation.
AIRCRAFT_CODE_MAP: dict[str, str | None] = {}

OUTPUT_FIELDS = [
    "airline_code", "origin", "destination",
    "aircraft_type", "flight_number", "departure_time", "ticket_price",
]

# day1–day7 map to Python weekday: 0 = Monday, 6 = Sunday
DAY_KEYS = ["day1", "day2", "day3", "day4", "day5", "day6", "day7"]


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--routes",   required=True, help="Path to europe_air_routes.csv")
    parser.add_argument("--airports", required=True, help="Path to airports.csv (supported airports)")
    parser.add_argument("--output",   required=True, help="Path to write output CSV")
    parser.add_argument("--date",     default="2026-07-14", help="Target flight date (YYYY-MM-DD)")
    parser.add_argument("--limit",    type=int, help="Stop after this many routes produce output (for testing)")
    return parser.parse_args()


def load_supported_airports(path):
    supported = set()
    with open(path) as f:
        for row in csv.DictReader(f):
            code = row["iata_code"].strip()
            if code:
                supported.add(code)
    return supported


def parse_flights_per_day(val):
    """Return daily flight count from strings like '0-2 flights', '3 flights', '0'.

    Takes the upper bound of any range; floors at 1 (routes with 0 flights are
    treated as operating at minimum once per day when the day flag is set).
    """
    nums = re.findall(r"\d+", val or "")
    upper = max((int(n) for n in nums), default=0)
    return max(1, upper)


def parse_route_date(val):
    """Parse M/D/YYYY date strings. Returns None if missing or zero."""
    val = (val or "").strip()
    if not val or val == "0":
        return None
    try:
        return datetime.strptime(val, "%m/%d/%Y").date()
    except ValueError:
        return None


def operating_weekdays(row):
    """Return the set of Python weekday ints (0=Mon, 6=Sun) on which this route operates."""
    return {i for i, key in enumerate(DAY_KEYS) if row.get(key, "").strip().lower() == "yes"}


def departure_time(route_id, total_flights, flight_idx):
    """Spread flights evenly across 06:00–22:00, with a per-route offset for variety."""
    start = 6 * 60   # 06:00 in minutes
    end   = 22 * 60  # 22:00 in minutes
    window = end - start

    # Stable per-route jitter so routes don't all depart on the hour
    jitter = (int(route_id) * 37) % 30

    if total_flights == 1:
        slot_minutes = window // 3
    else:
        slot_minutes = (window // (total_flights + 1)) * (flight_idx + 1)

    total_minutes = start + slot_minutes + jitter
    total_minutes = min(total_minutes, end - 30)  # no later than 21:30
    h, m = divmod(total_minutes, 60)
    return f"{h:02d}:{m:02d}:00"


def flight_number(carrier, route_id, flight_idx):
    """Derive a stable 6-char flight number from carrier + route + daily index."""
    base = (int(route_id) + flight_idx * 1000) % 9000 + 1000
    return f"{carrier}{base}"


def main():
    args = parse_args()

    try:
        target = date.fromisoformat(args.date)
    except ValueError:
        print(f"Error: --date must be YYYY-MM-DD, got {args.date!r}", file=sys.stderr)
        sys.exit(1)

    target_weekday = target.weekday()

    supported = load_supported_airports(args.airports)
    print(f"Supported airports: {len(supported)}", file=sys.stderr)

    routes_read      = 0
    routes_matched   = 0
    skipped_airports = 0
    skipped_dates    = 0
    rows_written     = 0

    with open(args.routes) as fin, open(args.output, "w", newline="") as fout:
        reader = csv.DictReader(fin)
        writer = csv.DictWriter(fout, fieldnames=OUTPUT_FIELDS)
        writer.writeheader()

        for row in reader:
            if args.limit is not None and routes_matched >= args.limit:
                break
            routes_read += 1

            origin      = row["iata_from"].strip()
            destination = row["iata_to"].strip()

            if origin not in supported or destination not in supported:
                skipped_airports += 1
                continue

            first = parse_route_date(row["first_flight"])
            last  = parse_route_date(row["last_flight"])

            if first and target < first:
                skipped_dates += 1
                continue
            if last and target > last:
                skipped_dates += 1
                continue
            if target_weekday not in operating_weekdays(row):
                skipped_dates += 1
                continue

            price           = row["price"].strip() or None
            flights_per_day = parse_flights_per_day(row["flights_per_day"])
            route_id        = row["id"]

            try:
                carriers = ast.literal_eval(row["airlineroutes"])
            except Exception as e:
                print(f"Warning: could not parse airlineroutes for route {route_id}: {e}", file=sys.stderr)
                continue

            for carrier_entry in carriers:
                carrier = carrier_entry.get("carrier", "").strip()
                if not carrier:
                    continue

                aircraft_raw = carrier_entry.get("aircraft_codes", 0)
                # aircraft_codes can be a comma-separated list (e.g. "32S, CRJ") or 0 when unknown;
                # take the first code and map it to the planes table's IATA codes
                if isinstance(aircraft_raw, str) and aircraft_raw:
                    first_code = aircraft_raw.split(",")[0].strip()
                    aircraft = AIRCRAFT_CODE_MAP.get(first_code, first_code) if first_code else None
                else:
                    aircraft = None

                for flight_idx in range(flights_per_day):
                    writer.writerow({
                        "airline_code":   carrier,
                        "origin":         origin,
                        "destination":    destination,
                        "aircraft_type":  aircraft,
                        "flight_number":  flight_number(carrier, route_id, flight_idx),
                        "departure_time": departure_time(route_id, flights_per_day, flight_idx),
                        "ticket_price":   price,
                    })
                    rows_written += 1

            if rows_written > 0:
                routes_matched += 1

    print(f"Routes read:          {routes_read:,}", file=sys.stderr)
    print(f"Routes matched:       {routes_matched:,}", file=sys.stderr)
    print(f"Skipped (airports):   {skipped_airports:,}", file=sys.stderr)
    print(f"Skipped (not active): {skipped_dates:,}", file=sys.stderr)
    print(f"Rows written:         {rows_written:,}", file=sys.stderr)


if __name__ == "__main__":
    main()
