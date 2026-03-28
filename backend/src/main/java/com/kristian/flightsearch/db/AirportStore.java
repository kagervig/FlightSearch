package com.kristian.flightsearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sql.DataSource;

import com.kristian.flightsearch.models.Airport;

/*
 * DB-backed airport store. Replaces AirportFileReader with a lazy-loaded,
 * cached view of the airports table.
 */
public class AirportStore {

    private final DataSource dataSource;
    private HashMap<String, Airport> cache;

    public AirportStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * Loads all airports from the DB on first call and caches them.
     */
    public Airport[] getAirports() {
        ensureLoaded();
        return cache.values().toArray(new Airport[0]);
    }

    public Airport getAirportByCode(String code) {
        ensureLoaded();
        return cache.get(code);
    }

    public boolean isValidAirportCode(String code) {
        if (code == null || code.isBlank()) return false;
        ensureLoaded();
        return cache.containsKey(code);
    }

    /*
     * Returns airports whose city matches the given name (case-insensitive, partial match).
     */
    public Airport[] searchByCity(String cityName) {
        String sql = "SELECT airport_id, iata_code, icao_code, name, city, country, latitude, longitude, "
                + "utc_offset, timezone, elevation_ft, max_runway_length_ft "
                + "FROM airports WHERE city ILIKE ?";

        ArrayList<Airport> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + cityName + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Error searching airports by city: " + e.getMessage());
        }
        return results.toArray(new Airport[0]);
    }

    private void ensureLoaded() {
        if (cache != null) return;

        cache = new HashMap<>();
        String sql = "SELECT airport_id, iata_code, icao_code, name, city, country, latitude, longitude, "
                + "utc_offset, timezone, elevation_ft, max_runway_length_ft FROM airports";

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Airport airport = mapRow(rs);
                if (airport.getCode() != null) {
                    cache.put(airport.getCode(), airport);
                }
            }
            System.out.println("Loaded " + cache.size() + " airports from database");
        } catch (Exception e) {
            System.out.println("Error loading airports: " + e.getMessage());
        }
    }

    private Airport mapRow(ResultSet rs) throws Exception {
        String iataCode = rs.getString("iata_code");
        String icaoCode = rs.getString("icao_code");
        String name = rs.getString("name");
        String city = rs.getString("city");
        String country = rs.getString("country");
        double lat = rs.getDouble("latitude");
        double lon = rs.getDouble("longitude");
        double utcOffset = rs.getDouble("utc_offset");
        String timezone = rs.getString("timezone");
        int elevationFt = rs.getInt("elevation_ft");
        int runwayLengthFt = rs.getInt("max_runway_length_ft");

        return new Airport(iataCode, name, lat, lon, runwayLengthFt, elevationFt, city, country, icaoCode, timezone, utcOffset);
    }
}
