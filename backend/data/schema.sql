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
    airline_name   TEXT         NOT NULL,
    country        TEXT
);



-- ============================================================
--  4. FLIGHTS
--  Source: flights.csv  |  2,983,327 rows
--  Date range: 2026-04-01 → 2026-05-31
--
--  flight_number identifies a scheduled service (same route,
--  time, and aircraft every day); it is NOT globally unique —
--  the same number recurs on each operating date.
--  The surrogate PK flight_id provides a unique row handle.
-- ============================================================
CREATE TABLE flights (
    flight_id      BIGSERIAL     PRIMARY KEY,
    flight_date    DATE          NOT NULL,
    airline_code   VARCHAR(3)    NOT NULL  REFERENCES airlines (airline_code),
    origin         CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    destination    CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    stops          SMALLINT      NOT NULL  DEFAULT 0  CHECK (stops BETWEEN 0 AND 10),
    aircraft_type  VARCHAR(4)              REFERENCES planes   (iata_code),
    flight_number  VARCHAR(8)    NOT NULL,
    departure_time TIME          NOT NULL,
    ticket_price   NUMERIC(8,2)  NOT NULL  CHECK (ticket_price > 0)
);

CREATE INDEX idx_flights_date          ON flights (flight_date);
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

-- \COPY flights (flight_date, airline_code, origin, destination, stops,
--               aircraft_type, flight_number, departure_time, ticket_price)
--   FROM 'flights.csv' CSV HEADER NULL AS '';


-- ============================================================
--  OPTIONAL: MONTH PARTITIONING  (recommended if data grows)
--
--    CREATE TABLE flights (...)
--      PARTITION BY RANGE (flight_date);
--
--    CREATE TABLE flights_2026_04 PARTITION OF flights
--      FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
--
--    CREATE TABLE flights_2026_05 PARTITION OF flights
--      FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
--
--  Indexes on the parent are automatically applied to each
--  partition; each month's data sits in a separate physical
--  segment, which greatly speeds up date-range scans.
-- ============================================================


-- ============================================================
--  OPTIONAL: SCHEDULES NORMALISATION
--
--  flights contains ~48,000 unique flight numbers, each
--  repeating across ~61 operating dates.  Splitting into a
--  schedules table (48k rows) and a flight_prices table
--  (2.9 M rows) eliminates the repeated route/time metadata:
--
--    CREATE TABLE schedules (
--        flight_number  VARCHAR(8)   PRIMARY KEY,
--        airline_code   VARCHAR(3)   NOT NULL  REFERENCES airlines,
--        origin         CHAR(3)      NOT NULL  REFERENCES airports,
--        destination    CHAR(3)      NOT NULL  REFERENCES airports,
--        departure_time TIME         NOT NULL,
--        stops          SMALLINT     NOT NULL  DEFAULT 0,
--        aircraft_type  VARCHAR(4)             REFERENCES planes
--    );
--
--    CREATE TABLE flight_prices (
--        flight_id      BIGSERIAL    PRIMARY KEY,
--        flight_number  VARCHAR(8)   NOT NULL  REFERENCES schedules,
--        flight_date    DATE         NOT NULL,
--        ticket_price   NUMERIC(8,2) NOT NULL,
--        UNIQUE (flight_number, flight_date)
--    );
-- ============================================================
