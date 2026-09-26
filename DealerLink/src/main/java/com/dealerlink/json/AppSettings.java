package com.dealerlink.json;

/**
 * Application settings read from the "settings" block of data/dealerlink-data.json.
 * Every field has a sensible default, so the app still runs if the JSON file (or a
 * single key in it) is missing. Jackson fills this POJO through its setters.
 */
public class AppSettings {

    private String appName = "DealerLink";
    private String currencySymbol = "৳"; // ৳
    private int lowStockThreshold = 10;
    private int orderMonitorIntervalSeconds = 10;
    private int inventoryCheckIntervalSeconds = 30;
    private String weatherApiUrl = "https://api.open-meteo.com/v1/forecast";
    private String geocodingApiUrl = "https://geocoding-api.open-meteo.com/v1/search";

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public int getOrderMonitorIntervalSeconds() { return orderMonitorIntervalSeconds; }
    public void setOrderMonitorIntervalSeconds(int v) { this.orderMonitorIntervalSeconds = Math.max(2, v); }
    public int getInventoryCheckIntervalSeconds() { return inventoryCheckIntervalSeconds; }
    public void setInventoryCheckIntervalSeconds(int v) { this.inventoryCheckIntervalSeconds = Math.max(5, v); }
    public String getWeatherApiUrl() { return weatherApiUrl; }
    public void setWeatherApiUrl(String weatherApiUrl) { this.weatherApiUrl = weatherApiUrl; }
    public String getGeocodingApiUrl() { return geocodingApiUrl; }
    public void setGeocodingApiUrl(String geocodingApiUrl) { this.geocodingApiUrl = geocodingApiUrl; }
}
