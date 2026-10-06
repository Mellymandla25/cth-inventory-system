package com.cth.ui;


import com.cth.database.DatabaseConnection;
import com.cth.security.SessionManager;
import com.cth.utils.AuditLogger;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ProductManagementDialog extends JDialog {
    private boolean productSaved = false;
    private Integer productId;
    private JTextField barcodeField;
    private JTextField nameField;
    private JComboBox<String> categoryCombo;
    private JTextField costPriceField;
    private JTextField sellingPriceField;
    private JTextField quantityField;
    private JTextField supplierField;
    private JTextField thresholdField;
    
    public ProductManagementDialog(Frame owner, Integer productId) {
        super(owner, productId == null ? "Add Product" : "Edit Product", true);
        this.productId = productId;
        
        setSize(500, 500);
        setLocationRelativeTo(owner);
        initUI();
        
        if (productId != null) {
            loadProductData();
        }
    }
    
    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Barcode
        panel.add(new JLabel("Barcode:*"), gbc);
        gbc.gridx = 1;
        barcodeField = new JTextField(20);
        panel.add(barcodeField, gbc);
        
        // Name
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Product Name:*"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        // Category
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Category:*"), gbc);
        gbc.gridx = 1;
        String[] categories = {"Welding", "PPE", "Power Tools", "Hand Tools", 
                              "Fasteners", "Paint", "Plumbing", "Other"};
        categoryCombo = new JComboBox<>(categories);
        panel.add(categoryCombo, gbc);
        
        // Cost Price
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Cost Price (R):*"), gbc);
        gbc.gridx = 1;
        costPriceField = new JTextField(20);
        panel.add(costPriceField, gbc);
        
        // Selling Price
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Selling Price (R):*"), gbc);
        gbc.gridx = 1;
        sellingPriceField = new JTextField(20);
        panel.add(sellingPriceField, gbc);
        
        // Quantity
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Initial Quantity:"), gbc);
        gbc.gridx = 1;
        quantityField = new JTextField("0", 20);
        panel.add(quantityField, gbc);
        
        // Low Stock Threshold
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Low Stock Threshold:"), gbc);
        gbc.gridx = 1;
        thresholdField = new JTextField("5", 20);
        panel.add(thresholdField, gbc);
        
        // Supplier
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        supplierField = new JTextField(20);
        panel.add(supplierField, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> saveProduct());
        cancelBtn.addActionListener(e -> dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, gbc);
        
        add(panel);
    }
    
    private void loadProductData() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM products WHERE id = ?")) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                barcodeField.setText(rs.getString("barcode"));
                nameField.setText(rs.getString("name"));
                categoryCombo.setSelectedItem(rs.getString("category"));
                costPriceField.setText(String.valueOf(rs.getDouble("cost_price")));
                sellingPriceField.setText(String.valueOf(rs.getDouble("selling_price")));
                quantityField.setText(String.valueOf(rs.getInt("quantity")));
                thresholdField.setText(String.valueOf(rs.getInt("low_stock_threshold")));
                supplierField.setText(rs.getString("supplier"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void saveProduct() {
        // Validate
        if (barcodeField.getText().trim().isEmpty() ||
            nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Barcode and Product Name are required.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            double costPrice = Double.parseDouble(costPriceField.getText().trim());
            double sellingPrice = Double.parseDouble(sellingPriceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            int threshold = Integer.parseInt(thresholdField.getText().trim());
            
            if (costPrice < 0 || sellingPrice < 0 || quantity < 0 || threshold < 0) {
                JOptionPane.showMessageDialog(this,
                    "All numeric values must be positive.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try (Connection conn = DatabaseConnection.getConnection()) {
                if (productId == null) {
                    // Insert new product
                    try (PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO products (barcode, name, category, cost_price, " +
                         "selling_price, quantity, low_stock_threshold, supplier) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        
                        pstmt.setString(1, barcodeField.getText().trim());
                        pstmt.setString(2, nameField.getText().trim());
                        pstmt.setString(3, (String) categoryCombo.getSelectedItem());
                        pstmt.setDouble(4, costPrice);
                        pstmt.setDouble(5, sellingPrice);
                        pstmt.setInt(6, quantity);
                        pstmt.setInt(7, threshold);
                        pstmt.setString(8, supplierField.getText().trim());
                        pstmt.executeUpdate();
                    }
                    
                    AuditLogger.log(SessionManager.getInstance().getUserId(),
                        SessionManager.getInstance().getUsername(),
                        "Product Added",
                        "Added product: " + nameField.getText().trim());
                    
                } else {
                    // Update existing product
                    try (PreparedStatement pstmt = conn.prepareStatement(
                         "UPDATE products SET barcode = ?, name = ?, category = ?, " +
                         "cost_price = ?, selling_price = ?, quantity = ?, " +
                         "low_stock_threshold = ?, supplier = ?, updated_at = CURRENT_TIMESTAMP " +
                         "WHERE id = ?")) {
                        
                        pstmt.setString(1, barcodeField.getText().trim());
                        pstmt.setString(2, nameField.getText().trim());
                        pstmt.setString(3, (String) categoryCombo.getSelectedItem());
                        pstmt.setDouble(4, costPrice);
                        pstmt.setDouble(5, sellingPrice);
                        pstmt.setInt(6, quantity);
                        pstmt.setInt(7, threshold);
                        pstmt.setString(8, supplierField.getText().trim());
                        pstmt.setInt(9, productId);
                        pstmt.executeUpdate();
                    }
                    
                    AuditLogger.log(SessionManager.getInstance().getUserId(),
                        SessionManager.getInstance().getUsername(),
                        "Product Updated",
                        "Updated product ID: " + productId);
                }
            }
            
            productSaved = true;
            JOptionPane.showMessageDialog(this, "Product saved successfully!");
            dispose();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter valid numbers for price, quantity, and threshold.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this,
                    "Barcode already exists. Please use a unique barcode.",
                    "Duplicate Error",
                    JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Database error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
            e.printStackTrace();
        }
    }
    
    public boolean isProductSaved() {
        return productSaved;
    }
}