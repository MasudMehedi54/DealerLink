package com.dealerlink.service;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Calls the free Open-Meteo REST API (no API key required) to fetch current weather
 * for a given latitude/longitude. Used to help decide whether a delivery route/dealer
 * choice might be affected by bad weather. Runs off the JavaFX thread via CompletableFuture.
 */
public class WeatherService {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static CompletableFuture<String> getCurrentWeatherAsync(double lat, double lon) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format(
                        "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                        lat, lon);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return "Weather unavailable (HTTP " + response.statusCode() + ")";
                }
                JSONObject json = new JSONObject(response.body());
                JSONObject current = json.getJSONObject("current_weather");
                double temp = current.getDouble("temperature");
                double windSpeed = current.getDouble("windspeed");
                return String.format("%.1f°C, wind %.1f km/h", temp, windSpeed);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Weather unavailable (" + e.getMessage() + ")";
            }
        });
    }
}
