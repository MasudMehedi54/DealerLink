package com.dealerlink.model;

public class Product {
    private int id;
    private String name;
    private String category;
    private String unit;

    public Product() {}

    public Product(int id, String name, String category, String unit) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() { return name; }
}
