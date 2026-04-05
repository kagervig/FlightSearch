CREATE TABLE IF NOT EXISTS airports (
    airport_id           INTEGER        PRIMARY KEY,
    iata_code            CHAR(3)        UNIQUE,
    icao_code            CHAR(4),
    name                 TEXT           NOT NULL,
    city                 TEXT,
    country              TEXT,
    latitude             NUMERIC(9,6),
    longitude            NUMERIC(9,6),
    utc_offset           NUMERIC(4,1),
    dst                  CHAR(1),
    timezone             TEXT,
    elevation_ft         INTEGER,
    max_runway_length_ft INTEGER
);

CREATE INDEX IF NOT EXISTS idx_airports_country ON airports (country);
CREATE INDEX IF NOT EXISTS idx_airports_iata    ON airports (iata_code);
CREATE INDEX IF NOT EXISTS idx_city             ON airports (city);
