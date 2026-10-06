package com.cth.models;


import java.sql.Timestamp;

public class Product {
    private int id;
    private String barcode;
    private String name;
    private String category;
    private double costPrice;
    private double sellingPrice;
    private int quantity;
    private int lowStockThreshold;
    private String supplier;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructors
    public Product() {}
    
    public Product(String barcode, String name, String category, 
                   double costPrice, double sellingPrice, int quantity) {
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.lowStockThreshold = 5;
        this.isActive = true;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
    
    public double getProfitMargin() {
        return sellingPrice - costPrice;
    }
    
    public double getProfitMarginPercentage() {
        return costPrice > 0 ? (getProfitMargin() / costPrice) * 100 : 0;
    }
    
    @Override
    public String toString() {
        return name + " (" + barcode + ") - Qty: " + quantity;
    }
}