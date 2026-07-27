#!/usr/bin/env python3
"""
Produce flights_2026-07-01.csv from two source files, normalised to the
flights table schema with deduplicated flight numbers.

Sources:
  1. flights_2026-07-14.csv  — full aircraft names, airline names
  2. all_airports_departures.csv — short aircraft codes, airline IATA codes

Flight number conflict resolution: if a number is already taken, set the
first digit of the numeric part to 9, then increment by 1 until free.
  e.g. AC1001 → conflict → AC9001 → if taken → AC9002 → …

Output columns (flights table order):
  flight_date, airline_code, origin, destination, stops,
  aircraft_type, flight_number, departure_time, ticket_price
"""

import csv
import re
import sys

FLIGHTS_FILE = "/Users/kristianallin/Code/FlightSearch/flights_2026-07-14.csv"
DEPARTS_FILE = "/Users/kristianallin/Downloads/all_airports_departures.csv"
AIRLINES_CSV = "/Users/kristianallin/Code/FlightSearch/backend/data/airlines.csv"
OUTPUT_FILE  = "/Users/kristianallin/Code/FlightSearch/flights_2026-07-01.csv"

OUTPUT_FIELDS = [
    "airline_code", "origin", "destination",
    "aircraft_type", "flight_number", "departure_time", "ticket_price",
]

# Full aircraft names (flights_2026-07-14.csv) → planes table IATA codes
AIRCRAFT_NAME_MAP = {
    "Aerospatiale/Alenia ATR 42-500": "AT5",
    "Aerospatiale/Alenia ATR 42-600": "ATR",
    "Aerospatiale/Alenia ATR 72":     "AT7",
    "Airbus A310":      "310", "Airbus A318":      "318",
    "Airbus A319":      "319", "Airbus A320":      "320",
    "Airbus A321":      "321", "Airbus A330":      "330",
    "Airbus A330-200":  "332", "Airbus A330-300":  "333",
    "Airbus A340":      "340", "Airbus A340-200":  "342",
    "Airbus A340-300":  "343", "Airbus A340-500":  "345",
    "Airbus A340-600":  "346", "Airbus A380":      "380",
    "Airbus A380-800":  "388", "Antonov AN-24":    "AN4",
    "Avro RJ100":       "AR1", "Avro RJ85":        "AR8",
    "BAe 146":          "146", "Beechcraft 1900":  "BEH",
    "Boeing 737":       "737", "Boeing 737-200":   "732",
    "Boeing 737-300":   "733", "Boeing 737-400":   "734",
    "Boeing 737-500":   "735", "Boeing 737-600":   "736",
    "Boeing 737-700":   "73G", "Boeing 737-800":   "738",
    "Boeing 737-900":   "739", "Boeing 747":       "747",
    "Boeing 747-400":   "744", "Boeing 757":       "757",
    "Boeing 757-200":   "752", "Boeing 757-300":   "753",
    "Boeing 767":       "767", "Boeing 767-300":   "763",
    "Boeing 767-400":   "764", "Boeing 777":       "777",
    "Boeing 777-200":   "772", "Boeing 777-200LR": "77L",
    "Boeing 777-300":   "773", "Boeing 777-300ER": "77W",
    "Boeing 787":       "787", "Boeing 787-8":     "788",
    "Canadair Regional Jet 200": "CR2",
    "Canadair Regional Jet 700": "CR7",
    "Canadair Regional Jet 900": "CR9",
    "Embraer 170":              "E70",
    "Embraer 175":              "E75",
    "Embraer 190":              "E90",
    "Embraer 195":              "E95",
    "Embraer EMB 120 Brasilia": "EM2",
    "Embraer RJ140":            "ERD",
    "Embraer RJ145":            "ER4",
    "Fokker 50":                "F50",
}

# Short codes (all_airports_departures.csv) → planes table IATA codes
AIRCRAFT_CODE_MAP = {
    "A320": "320", "A321": "321",
    "B737": "737", "B737-800": "738",
    "E190": "E90", "B777": "777",
    "A330": "330", "B787": "787",
    "A350": "350", "Q400": "DH8",
    "Dash 8": "DH8",
}


def load_airlines(path):
    """Return (name_lower → code, code_set)."""
    name_map, codes = {}, set()
    with open(path) as f:
        for row in csv.DictReader(f):
            code = row["Airline Code"].strip()
            name_map[row["Airline Name"].strip().lower()] = code
            codes.add(code)
    return name_map, codes


def normalise_time(t):
    t = t.strip()
    return t + ":00" if len(t) == 5 else t


def resolve_conflict(fn, used):
    """Return fn unchanged if not in used; otherwise find the next free number."""
    if fn not in used:
        return fn

    m = re.match(r'^([A-Za-z]*)(\d+)(.*)$', fn)
    if not m:
        i = 1
        while f"{fn}_{i}" in used:
            i += 1
        return f"{fn}_{i}"

    prefix, digits, suffix = m.group(1), m.group(2), m.group(3)
    # set first digit to 9, preserving digit-count padding if needed
    candidate_digits = "9" + digits[1:]
    candidate = prefix + candidate_digits + suffix

    while candidate in used:
        n = int(candidate_digits) + 1
        candidate_digits = str(n)
        candidate = prefix + candidate_digits + suffix

    return candidate


def main():
    airline_name_map, valid_codes = load_airlines(AIRLINES_CSV)

    rows = []
    skipped_airline = []

    # ── Source 1: flights_2026-07-14.csv ──────────────────────────────────
    with open(FLIGHTS_FILE) as f:
        for row in csv.DictReader(f):
            if row["stops"].strip() != "0":
                continue
            code = airline_name_map.get(row["airline_name"].strip().lower())
            if not code:
                skipped_airline.append(("file1", row["airline_name"]))
                continue
            rows.append({
                "airline_code":   code,
                "origin":         row["origin"].strip(),
                "destination":    row["destination"].strip(),
                "aircraft_type":  AIRCRAFT_NAME_MAP.get(row["aircraft_type"].strip()),
                "flight_number":  row["flight_number"].strip(),
                "departure_time": normalise_time(row["departure_time"]),
                "ticket_price":   row["ticket_price"].strip(),
            })

    s1_count = len(rows)
    print(f"Source 1: {s1_count:,} rows loaded", file=sys.stderr)

    # ── Source 2: all_airports_departures.csv ─────────────────────────────
    s2_loaded = 0
    with open(DEPARTS_FILE) as f:
        for row in csv.DictReader(f):
            if row["Stops"].strip() != "0":
                continue
            code = row["Airline Code"].strip()
            if code not in valid_codes:
                skipped_airline.append(("file2", code))
                continue
            ac_raw = row["Aircraft Type"].strip()
            rows.append({
                "airline_code":   code,
                "origin":         row["Origin"].strip(),
                "destination":    row["Destination"].strip(),
                "aircraft_type":  AIRCRAFT_CODE_MAP.get(ac_raw, ac_raw),
                "flight_number":  row["Flight Number"].strip(),
                "departure_time": normalise_time(row["Departure Time"]),
                "ticket_price":   row["Estimated Price (EUR)"].strip(),
            })
            s2_loaded += 1

    print(f"Source 2: {s2_loaded:,} rows loaded", file=sys.stderr)

    if skipped_airline:
        from collections import Counter
        counts = Counter(v for _, v in skipped_airline)
        print(f"Skipped {len(skipped_airline)} rows — unrecognised airline:", file=sys.stderr)
        for name, n in counts.most_common():
            print(f"  {n:4}  {name}", file=sys.stderr)

    # ── Deduplicate flight numbers ─────────────────────────────────────────
    used = set()
    conflicts = 0
    for row in rows:
        fn = row["flight_number"]
        resolved = resolve_conflict(fn, used)
        if resolved != fn:
            conflicts += 1
        row["flight_number"] = resolved
        used.add(resolved)

    print(f"Conflicts resolved: {conflicts}", file=sys.stderr)
    print(f"Total rows: {len(rows):,}", file=sys.stderr)

    # ── Write output ───────────────────────────────────────────────────────
    with open(OUTPUT_FILE, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=OUTPUT_FIELDS)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Written: {OUTPUT_FILE}", file=sys.stderr)


if __name__ == "__main__":
    main()
