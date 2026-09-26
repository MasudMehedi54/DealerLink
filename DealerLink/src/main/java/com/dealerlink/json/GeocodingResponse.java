package com.dealerlink.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed Jackson mapping of the Open-Meteo geocoding API response:
 * { "results": [ { "name": "Khulna", "latitude": 22.8098, "longitude": 89.5644,
 *                  "country": "Bangladesh", ... } ] }
 * When nothing matches, the API omits "results" entirely - the list then stays empty.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingResponse {

    private List<Place> results = new ArrayList<>();

    public List<Place> getResults() { return results; }
    public void setResults(List<Place> results) { this.results = results == null ? new ArrayList<>() : results; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String name;
        private String country;
        private double latitude;
        private double longitude;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }
}
