package com.dealerlink.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private int id;
    private String username;
    private String password;
    private String role;      // "SHOP" or "DEALER"
    private String fullName;
    private String location;
    private double latitude;
    private double longitude;

    public User() {}

    public User(int id, String username, String password, String role,
                String fullName, String location, double latitude, double longitude) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @JsonIgnore
    public boolean isDealer() { return "DEALER".equalsIgnoreCase(role); }
    @JsonIgnore
    public boolean isShop() { return "SHOP".equalsIgnoreCase(role); }

    @Override
    public String toString() { return fullName + " (" + username + ")"; }
}
