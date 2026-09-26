package com.dealerlink.model;

public class Delivery {
    private int id;
    private int orderId;
    private String status; // PREPARING, IN_TRANSIT, DELIVERED
    private String eta;
    private String currentLocation;
    private String updatedAt;

    public Delivery() {}

    public Delivery(int id, int orderId, String status, String eta, String currentLocation, String updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.eta = eta;
        this.currentLocation = currentLocation;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
