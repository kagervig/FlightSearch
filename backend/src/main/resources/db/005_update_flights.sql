-- Removes date-specific columns from the flights table.
-- The table must be re-seeded from the new flights.csv after this migration runs.
ALTER TABLE IF EXISTS flights DROP COLUMN IF EXISTS flight_date;
ALTER TABLE IF EXISTS flights DROP COLUMN IF EXISTS stops;
DROP INDEX IF EXISTS idx_flights_date;
