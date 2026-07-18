CREATE TABLE IF NOT EXISTS flights (
    flight_id      BIGSERIAL     PRIMARY KEY,
    airline_code   VARCHAR(3)    NOT NULL  REFERENCES airlines (airline_code),
    origin         CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    destination    CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    aircraft_type  VARCHAR(4)              REFERENCES planes (iata_code),
    flight_number  VARCHAR(8)    NOT NULL  UNIQUE,
    departure_time TIME          NOT NULL,
    ticket_price   NUMERIC(8,2)  NOT NULL  CHECK (ticket_price > 0)
);

CREATE INDEX IF NOT EXISTS idx_flights_origin      ON flights (origin);
CREATE INDEX IF NOT EXISTS idx_flights_destination ON flights (destination);
