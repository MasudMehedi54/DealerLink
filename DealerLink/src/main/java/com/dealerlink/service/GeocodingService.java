package com.dealerlink.service;

import com.dealerlink.dao.UserDAO;
import com.dealerlink.json.CityCoordinate;
import com.dealerlink.json.GeocodingResponse;
import com.dealerlink.json.JsonDataService;
import com.dealerlink.model.User;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Turns a typed city name ("Khulna", "Dhaka, Bangladesh") into latitude/longitude.
 *
 * Lookup order:
 *   1. the offline "cities" table in data/dealerlink-data.json (fast, works without internet)
 *   2. the free Open-Meteo geocoding REST API, parsed with Jackson into GeocodingResponse
 *
 * Accounts created on the Register screen start with lat/lon = 0,0; ensureCoordinates()
 * fills them in the first time they're needed and saves them to the database.
 * All methods block on network I/O - call them from a background thread only.
 */
public final class GeocodingService {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final UserDAO userDAO = new UserDAO();

    private GeocodingService() {}

    public static boolean hasCoordinates(User u) {
        return u != null && !(u.getLatitude() == 0 && u.getLongitude() == 0);
    }

    /** Finds coordinates for a place name, or empty if it can't be resolved. */
    public static Optional<double[]> lookup(String location) {
        if (location == null || location.isBlank()) return Optional.empty();
        String city = location.split(",")[0].trim();

        // 1) Offline table from the JSON file
        for (CityCoordinate c : JsonDataService.load().getCities()) {
            if (c.getName() != null && c.getName().equalsIgnoreCase(city)) {
                return Optional.of(new double[]{c.getLatitude(), c.getLongitude()});
            }
        }

        // 2) Open-Meteo geocoding API
        try {
            String url = JsonDataService.settings().getGeocodingApiUrl()
                    + "?count=1&language=en&format=json&name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return Optional.empty();
            GeocodingResponse parsed = JsonDataService.mapper().readValue(res.body(), GeocodingResponse.class);
            if (parsed.getResults().isEmpty()) return Optional.empty();
            GeocodingResponse.Place p = parsed.getResults().get(0);
            return Optional.of(new double[]{p.getLatitude(), p.getLongitude()});
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("[DealerLink] Geocoding failed for '" + city + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Makes sure the user has coordinates: if they're 0,0, geocodes the user's location,
     * updates the User object AND the database. Returns true if coordinates are available.
     */
    public static boolean ensureCoordinates(User u) {
        if (u == null) return false;
        if (hasCoordinates(u)) return true;
        Optional<double[]> found = lookup(u.getLocation());
        if (found.isEmpty()) return false;
        u.setLatitude(found.get()[0]);
        u.setLongitude(found.get()[1]);
        if (u.getId() > 0) userDAO.updateCoordinates(u.getId(), u.getLatitude(), u.getLongitude());
        return true;
    }
}
