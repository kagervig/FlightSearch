CREATE TABLE IF NOT EXISTS flights (
    flight_id      BIGSERIAL     PRIMARY KEY,
    flight_date    DATE          NOT NULL,
    airline_code   VARCHAR(3)    NOT NULL  REFERENCES airlines (airline_code),
    origin         CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    destination    CHAR(3)       NOT NULL  REFERENCES airports (iata_code),
    stops          SMALLINT      NOT NULL  DEFAULT 0  CHECK (stops BETWEEN 0 AND 10),
    aircraft_type  VARCHAR(4)              REFERENCES planes (iata_code),
    flight_number  VARCHAR(8)    NOT NULL,
    departure_time TIME          NOT NULL,
    ticket_price   NUMERIC(8,2)  NOT NULL  CHECK (ticket_price > 0)
);

CREATE INDEX IF NOT EXISTS idx_flights_date        ON flights (flight_date);
CREATE INDEX IF NOT EXISTS idx_flights_origin      ON flights (origin);
CREATE INDEX IF NOT EXISTS idx_flights_destination ON flights (destination);
