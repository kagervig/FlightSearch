# Todo

### Deployment
- [ ] Get Render working

### Database — Real-World Data (OpenFlights)
- [ ] Create DB migrations for new tables: `airports`, `airlines`, `planes`, `routes`
- [ ] Write importer for `airports.txt` → `airports` table (14-field CSV: id, name, city, country, iata, icao, lat, lon, elevation, timezone, DST, tz_db, type, source)
- [ ] Write importer for `airlines.txt` → `airlines` table (8-field CSV: id, name, alias, iata, icao, callsign, country, active)
- [ ] Write importer for `planes.txt` → `planes` table (3-field CSV: name, iata_code, icao_code)
- [ ] Write importer for `routes.txt` → `routes` table (9-field CSV: airline, airline_id, origin, origin_id, dest, dest_id, codeshare, stops, equipment)
- [ ] Augment `routes` with computed distance, duration, and base price using the existing pricing algorithm
- [ ] Update `Airport` model to match new schema (add icao, timezone; runway length not available in new data)
- [ ] Replace `AirportFileReader` (609airports.txt) with DB-backed airport lookup
- [ ] Update `Server.java` data loading to use new DB tables instead of old file readers
- [ ] Fix `MultiCitySearch.java` to use database instead of static `flights.txt`

### Flight Schedule (future)
- [ ] Generate a full-year flight schedule from routes: weekly-repeating pattern, price as the main daily variable, stored in `flights` table

### Features
- [ ] Duration optimization: frontend has Price/Duration toggle but backend always sorts by price
- [ ] City-name search — users shouldn't need to know airport codes
- [ ] Authentication: previous search history, default home airport per user
- [ ] Wire up `Airline` model into flight pricing and display
- [ ] Return-flight validation for multi-city routes

### UI
- [ ] Upgrade the UI (design TBD)

### Testing
- [ ] Add integration tests for API endpoints
- [ ] Add frontend tests
