package com.dealerlink.service;

import com.dealerlink.json.JsonDataService;
import com.dealerlink.json.WeatherResponse;
import com.dealerlink.model.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Calls the free Open-Meteo REST API (no API key required) to fetch current weather
 * for a given latitude/longitude. The JSON response is parsed with Jackson straight
 * into the typed {@link WeatherResponse} class. The base URL comes from the
 * "settings.weatherApiUrl" key of data/dealerlink-data.json.
 * Runs off the JavaFX thread via CompletableFuture.
 */
public class WeatherService {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * Weather for a user's location. If the account has no coordinates yet (e.g. it was
     * created on the Register screen), they're looked up from the user's city first via
     * GeocodingService and saved, so later calls are instant.
     */
    public static CompletableFuture<String> getWeatherForUserAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (!GeocodingService.ensureCoordinates(user)) {
                return "Couldn't find the location '" + user.getLocation()
                        + "'. Check the city spelling (e.g. Khulna, Dhaka).";
            }
            return fetch(user.getLatitude(), user.getLongitude());
        });
    }

    public static CompletableFuture<String> getCurrentWeatherAsync(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> fetch(lat, lon));
    }

    /** Blocking HTTP call + Jackson parsing; always run it off the JavaFX thread. */
    private static String fetch(double lat, double lon) {
            try {
                String url = String.format(Locale.US, "%s?latitude=%.4f&longitude=%.4f&current_weather=true",
                        JsonDataService.settings().getWeatherApiUrl(), lat, lon);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return "Weather unavailable (HTTP " + response.statusCode() + ")";
                }
                WeatherResponse parsed = JsonDataService.mapper().readValue(response.body(), WeatherResponse.class);
                WeatherResponse.CurrentWeather cw = parsed.getCurrentWeather();
                if (cw == null) return "Weather unavailable (unexpected response)";
                return String.format("%s, %.1f°C, wind %.1f km/h", cw.describe(), cw.getTemperature(), cw.getWindSpeed());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Weather request interrupted";
            } catch (IOException e) {
                return "Weather unavailable (" + e.getMessage() + ")";
            }
    }
}
