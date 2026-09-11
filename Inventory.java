package com.dealerlink.model;

public class Inventory {
    private int id;
    private int dealerId;
    private int productId;
    private String productName; // convenience field for display (joined from products)
    private int quantity;
    private double unitPrice;

    public Inventory() {}

    public Inventory(int id, int dealerId, int productId, String productName, int quantity, double unitPrice) {
        this.id = id;
        this.dealerId = dealerId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getDealerId() { return dealerId; }
    public void setDealerId(int dealerId) { this.dealerId = dealerId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
