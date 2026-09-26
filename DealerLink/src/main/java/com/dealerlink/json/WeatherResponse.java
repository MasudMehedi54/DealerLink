package com.dealerlink.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Typed Jackson mapping of the part of the Open-Meteo response we use:
 * { "current_weather": { "temperature": 29.4, "windspeed": 7.2, "weathercode": 3 }, ... }
 * Everything else in the response is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    @JsonProperty("current_weather")
    private CurrentWeather currentWeather;

    public CurrentWeather getCurrentWeather() { return currentWeather; }
    public void setCurrentWeather(CurrentWeather currentWeather) { this.currentWeather = currentWeather; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentWeather {
        private double temperature;
        @JsonProperty("windspeed")
        private double windSpeed;
        @JsonProperty("weathercode")
        private int weatherCode;

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
        public int getWeatherCode() { return weatherCode; }
        public void setWeatherCode(int weatherCode) { this.weatherCode = weatherCode; }

        /** Human-readable text for the WMO weather code Open-Meteo returns. */
        public String describe() {
            int c = weatherCode;
            if (c == 0) return "Clear sky";
            if (c <= 3) return "Partly cloudy";
            if (c == 45 || c == 48) return "Fog";
            if (c >= 51 && c <= 67) return "Rain";
            if (c >= 71 && c <= 77) return "Snow";
            if (c >= 80 && c <= 82) return "Rain showers";
            if (c >= 95) return "Thunderstorm - deliveries may be delayed";
            return "Mixed conditions";
        }
    }
}
