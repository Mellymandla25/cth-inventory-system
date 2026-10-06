package com.cth.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int id;
    private String invoiceNumber;
    private int userId;
    private String customerName;
    private String customerPhone;
    private double subtotal;
    private double vatAmount;
    private double discount;
    private double totalAmount;
    private String paymentMethod;
    private Timestamp saleDate;
    private boolean voided;
    private Integer voidedBy;
    private Timestamp voidedAt;
    private List<SaleItem> items;
    
    // Constructors
    public Sale() {
        this.items = new ArrayList<>();
    }
    
    public Sale(String invoiceNumber, int userId, double subtotal, double vatAmount, double totalAmount) {
        this();
        this.invoiceNumber = invoiceNumber;
        this.userId = userId;
        this.subtotal = subtotal;
        this.vatAmount = vatAmount;
        this.totalAmount = totalAmount;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    
    public double getVatAmount() { return vatAmount; }
    public void setVatAmount(double vatAmount) { this.vatAmount = vatAmount; }
    
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public Timestamp getSaleDate() { return saleDate; }
    public void setSaleDate(Timestamp saleDate) { this.saleDate = saleDate; }
    
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
    
    public Integer getVoidedBy() { return voidedBy; }
    public void setVoidedBy(Integer voidedBy) { this.voidedBy = voidedBy; }
    
    public Timestamp getVoidedAt() { return voidedAt; }
    public void setVoidedAt(Timestamp voidedAt) { this.voidedAt = voidedAt; }
    
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public void addItem(SaleItem item) { this.items.add(item); }
    
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    public double getTotalItems() {
        return items != null ? items.stream().mapToInt(SaleItem::getQuantity).sum() : 0;
    }
    
    @Override
    public String toString() {
        return invoiceNumber + " - " + saleDate + " - Total: " + totalAmount;
    }
}