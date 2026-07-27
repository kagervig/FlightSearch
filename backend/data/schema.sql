-- ============================================================
--  Flight Data – PostgreSQL Schema
--  Coverage: April – May 2026  |  2,983,327 flights
--
--  Table load order (respects FK dependencies):
--    1. planes
--    2. airports
--    3. airlines
--    4. flights
-- ============================================================


-- ============================================================
--  1. PLANES
--  Source: planes.csv  |  57 rows  |  no header row
-- ============================================================
CREATE TABLE planes (
    iata_code   VARCHAR(4)   PRIMARY KEY,   -- e.g. '738', '32S'
    name        TEXT         NOT NULL       -- e.g. 'Boeing 737-800'
);


-- ============================================================
--  2. AIRPORTS
--  Source: airports.csv  |  1,789 rows
--
--  Notes:
--    • elevation_ft is the merged elevation column (OurAirports
--      value preferred; falls back to OpenFlights where missing).
--    • dst: DST rule zone — E=Europe, A=US/Canada, S=S.America,
--      O=Australia, Z=New Zealand, N=none, U=unknown.
--    • 'type' and 'source' columns present in CSV but not loaded
--      (every row is 'airport'; source is metadata only).
-- ============================================================
CREATE TABLE airports (
    airport_id           INTEGER        PRIMARY KEY,  -- OpenFlights numeric ID
    iata_code            CHAR(3)        UNIQUE,       -- 3-letter IATA code
    icao_code            CHAR(4),                    -- 4-letter ICAO code
    name                 TEXT           NOT NULL,
    city                 TEXT,
    country              TEXT,
    latitude             NUMERIC(9,6),
    longitude            NUMERIC(9,6),
    utc_offset           NUMERIC(4,1),               -- hours from UTC, e.g. -5.5
    dst                  CHAR(1),                    -- DST rule zone (see note above)
    timezone             TEXT,                       -- Olson tz, e.g. 'Europe/London'
    elevation_ft         INTEGER,                    -- airfield elevation in feet
    max_runway_length_ft INTEGER                     -- longest open runway in feet
);

CREATE INDEX idx_airports_country ON airports (country);
CREATE INDEX idx_airports_iata    ON airports (iata_code);
CREATE INDEX idx_city ON airports (city);


-- ============================================================
--  3. AIRLINES
--  Source: airlines.csv  |  162 rows
-- ============================================================
CREATE TABLE airlines (
    airline_code   VARCHAR(3)   PRIMARY KEY,   -- 2–3 char IATA designator
    airline_name   TEXT,
    country        TEXT
);



-- ============================================================
--  4. FLIGHTS
--  Source: flights.csv  |  ~50,000 rows (one per flight number)
--
--  Each row is a date-independent scheduled service. Dates are
--  computed in the application layer for display only.
--  flight_number is unique across the table.
-- ============================================================
CREATE TABLE flights (
    flight_id      BIGSERIAL     PRIMARY KEY,
    airline_code   VARCHAR(3)    NOT NULL  REFERENCES airlines (airline_code),
    origin         CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    destination    CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    aircraft_type  VARCHAR(4)              REFERENCES planes   (iata_code),
    flight_number  VARCHAR(8)    NOT NULL  UNIQUE,
    departure_time TIME          NOT NULL,
    ticket_price   NUMERIC(8,2)  NOT NULL  CHECK (ticket_price > 0)
);

CREATE INDEX idx_flights_origin        ON flights (origin);
CREATE INDEX idx_flights_destination   ON flights (destination);


-- ============================================================
--  LOAD COMMANDS  (run from psql as a superuser or table owner)
-- ============================================================

-- planes.csv has no header row
-- \COPY planes (name, iata_code)
--   FROM 'planes.csv' CSV NULL AS '';

-- \COPY airports (airport_id, name, city, country, iata_code, icao_code,
--                 latitude, longitude, utc_offset, dst, timezone,
--                 elevation_ft, max_runway_length_ft)
--   FROM 'airports.csv' CSV HEADER NULL AS '';

-- \COPY airlines (airline_code, airline_name, country)
--   FROM 'airlines.csv' CSV HEADER NULL AS '';

-- \COPY flights (airline_code, origin, destination,
--               aircraft_type, flight_number, departure_time, ticket_price)
--   FROM 'flights.csv' CSV HEADER NULL AS '';


