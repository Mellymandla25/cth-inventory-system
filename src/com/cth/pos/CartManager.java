package com.cth.pos;


import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private List<CartItem> items;
    private double subtotal;
    private double vatRate = 0.15;
    
    public CartManager() {
        this.items = new ArrayList<>();
        updateTotals();
    }
    
    public void addItem(int productId, String productName, double unitPrice, int quantity) {
        // Check if item already exists in cart
        for (CartItem item : items) {
            if (item.getProductId() == productId) {
                item.setQuantity(item.getQuantity() + quantity);
                updateTotals();
                return;
            }
        }
        
        // New item
        items.add(new CartItem(productId, productName, unitPrice, quantity));
        updateTotals();
    }
    
    public void updateQuantity(int index, int newQuantity) {
        if (index >= 0 && index < items.size()) {
            if (newQuantity <= 0) {
                items.remove(index);
            } else {
                items.get(index).setQuantity(newQuantity);
            }
            updateTotals();
        }
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            updateTotals();
        }
    }
    
    public void clearCart() {
        items.clear();
        updateTotals();
    }
    
    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public CartItem getItemAt(int index) {
        return items.get(index);
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public double getSubtotal() {
        return subtotal;
    }
    
    public double getVatAmount() {
        return subtotal * vatRate;
    }
    
    public double getTotal() {
        return subtotal + getVatAmount();
    }
    
    private void updateTotals() {
        subtotal = 0;
        for (CartItem item : items) {
            subtotal += item.getSubtotal();
        }
    }
    
    public double getVatRate() {
        return vatRate;
    }
    
    public void setVatRate(double vatRate) {
        this.vatRate = vatRate;
        updateTotals();
    }
    
    // Inner class for cart items
    public static class CartItem {
        private int productId;
        private String productName;
        private double unitPrice;
        private int quantity;
        
        public CartItem(int productId, String productName, double unitPrice, int quantity) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
        
        public int getProductId() { return productId; }
        public String getProductName() { return productName; }
        public double getUnitPrice() { return unitPrice; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getSubtotal() { return unitPrice * quantity; }
    }
}