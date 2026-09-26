package com.dealerlink.json;

/**
 * One stock line in the JSON file. It refers to the dealer by username and the
 * product by name (instead of numeric database ids) so the JSON stays readable
 * and hand-editable; JsonDataService resolves them to ids when seeding.
 */
public class InventorySeed {
    private String dealerUsername;
    private String productName;
    private int quantity;
    private double unitPrice;

    public InventorySeed() {}

    public String getDealerUsername() { return dealerUsername; }
    public void setDealerUsername(String dealerUsername) { this.dealerUsername = dealerUsername; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
