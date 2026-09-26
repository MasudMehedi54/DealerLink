package com.dealerlink.model;

public class ProductRequest {
    private int id;
    private int shopId;
    private int productId;
    private String productName;
    private int quantity;
    private String status; // OPEN, QUOTED, ORDERED, CLOSED
    private String createdAt;

    public ProductRequest() {}

    public ProductRequest(int id, int shopId, int productId, String productName,
                           int quantity, String status, String createdAt) {
        this.id = id;
        this.shopId = shopId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getShopId() { return shopId; }
    public void setShopId(int shopId) { this.shopId = shopId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
