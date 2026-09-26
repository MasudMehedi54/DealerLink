package com.dealerlink.model;

public class Quotation {
    private int id;
    private int requestId;
    private int dealerId;
    private String dealerName; // joined convenience field
    private double price;
    private int deliveryDays;
    private String status; // PENDING, ACCEPTED, REJECTED
    private String createdAt;

    public Quotation() {}

    public Quotation(int id, int requestId, int dealerId, String dealerName,
                      double price, int deliveryDays, String status, String createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.dealerId = dealerId;
        this.dealerName = dealerName;
        this.price = price;
        this.deliveryDays = deliveryDays;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public int getDealerId() { return dealerId; }
    public void setDealerId(int dealerId) { this.dealerId = dealerId; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(int deliveryDays) { this.deliveryDays = deliveryDays; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
