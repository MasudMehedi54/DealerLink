package com.dealerlink.model;

public class Order {
    private int id;
    private int requestId;
    private int quotationId;
    private String status; // PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    private String orderedAt;

    // convenience joined fields for display
    private String productName;
    private double price;
    private String dealerName;

    public Order() {}

    public Order(int id, int requestId, int quotationId, String status, String orderedAt) {
        this.id = id;
        this.requestId = requestId;
        this.quotationId = quotationId;
        this.status = status;
        this.orderedAt = orderedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public int getQuotationId() { return quotationId; }
    public void setQuotationId(int quotationId) { this.quotationId = quotationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrderedAt() { return orderedAt; }
    public void setOrderedAt(String orderedAt) { this.orderedAt = orderedAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDealerName() { return dealerName; }
    public void setDealerName(String dealerName) { this.dealerName = dealerName; }
}
