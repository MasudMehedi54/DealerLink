package com.dealerlink.json;

/** One entry of the "cities" list in data/dealerlink-data.json (offline geocoding table). */
public class CityCoordinate {
    private String name;
    private double latitude;
    private double longitude;

    public CityCoordinate() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
