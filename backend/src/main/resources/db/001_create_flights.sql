CREATE TABLE IF NOT EXISTS flights (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheduled_departure TIMESTAMP NOT NULL,
    scheduled_arrival   TIMESTAMP NOT NULL,
    flight_number       VARCHAR NOT NULL,
    origin              VARCHAR NOT NULL,
    destination         VARCHAR NOT NULL,
    price               INTEGER NOT NULL,
    currency            VARCHAR NOT NULL,
    duration            INTEGER NOT NULL
);
