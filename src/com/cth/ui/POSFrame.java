package com.cth.ui;

import com.cth.config.ConfigManager;
import com.cth.database.DatabaseConnection;
import com.cth.pos.CartManager;
import com.cth.pos.SaleProcessor;
import com.cth.security.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.text.DecimalFormat;

public class POSFrame extends JFrame {
    private CartManager cartManager;
    private SaleProcessor saleProcessor;
    private JTextField barcodeField;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel statusLabel;
    private JLabel subtotalLabel;
    private JLabel vatLabel;
    private JLabel totalLabel;
    private DecimalFormat currencyFormat = new DecimalFormat("R #,##0.00");
    private JButton completeBtn;
    private JButton clearBtn;
    private JPanel totalsPanel;
    private JPanel tablePanel;
    
    public POSFrame() {
        setTitle("CTH Tools - Point of Sale");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        
        cartManager = new CartManager();
        saleProcessor = new SaleProcessor();
        
        initUI();
        setupKeyBindings();
        setupSessionTimeout();
        
        barcodeField.requestFocusInWindow();
    }
    
    private void initUI() {
        // ============================================
        // MAIN PANEL
        // ============================================
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // ============================================
        // TOP PANEL
        // ============================================
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("POS - " + SessionManager.getInstance().getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 12));
        logoutBtn.addActionListener(e -> logout());
        topPanel.add(logoutBtn, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // ============================================
        // CENTER PANEL
        // ============================================
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Barcode entry
        JPanel entryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        entryPanel.setBorder(BorderFactory.createTitledBorder("Scan or Enter Barcode"));
        entryPanel.setBackground(new Color(240, 248, 255));
        
        barcodeField = new JTextField(25);
        barcodeField.setFont(new Font("Monospaced", Font.PLAIN, 18));
        barcodeField.setBackground(Color.WHITE);
        
        barcodeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    processBarcode(barcodeField.getText().trim());
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                }
            }
        });
        
        JButton addBtn = new JButton("Add to Cart");
        addBtn.setFont(new Font("Arial", Font.BOLD, 12));
        addBtn.addActionListener(e -> {
            processBarcode(barcodeField.getText().trim());
            barcodeField.setText("");
            barcodeField.requestFocusInWindow();
        });
        
        JButton quantityBtn = new JButton("Set Quantity");
        quantityBtn.setFont(new Font("Arial", Font.BOLD, 12));
        quantityBtn.addActionListener(e -> setQuantityForSelectedItem());
        
        entryPanel.add(new JLabel("Barcode:"));
        entryPanel.add(barcodeField);
        entryPanel.add(addBtn);
        entryPanel.add(quantityBtn);
        
        centerPanel.add(entryPanel, BorderLayout.NORTH);
        
        // ============================================
        // TABLE PANEL
        // ============================================
        tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Current Sale"));
        
        // Cart table
        String[] columns = {"Qty", "Product", "Unit Price", "Subtotal"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(30);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 14));
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        cartTable.getTableHeader().setBackground(new Color(200, 200, 200));
        
        JScrollPane tableScrollPane = new JScrollPane(cartTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 200));
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // ============================================
        // TOTALS + BUTTONS PANEL
        // ============================================
        JPanel bottomAreaPanel = new JPanel(new BorderLayout(10, 5));
        bottomAreaPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomAreaPanel.setBackground(new Color(240, 248, 255));
        
        // Totals (left side)
        totalsPanel = new JPanel(new GridLayout(3, 1, 3, 2));
        totalsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        totalsPanel.setBackground(new Color(240, 248, 255));
        
        subtotalLabel = new JLabel("Subtotal: R 0.00");
        subtotalLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        vatLabel = new JLabel("VAT (15%): R 0.00");
        vatLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        totalLabel = new JLabel("TOTAL: R 0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        totalLabel.setForeground(new Color(0, 120, 0));
        
        totalsPanel.add(subtotalLabel);
        totalsPanel.add(vatLabel);
        totalsPanel.add(totalLabel);
        
        bottomAreaPanel.add(totalsPanel, BorderLayout.WEST);
        
        // Right side - Buttons + Status
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        rightPanel.setBackground(new Color(240, 248, 255));
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        clearBtn = new JButton("Clear Cart");
        clearBtn.setFont(new Font("Arial", Font.BOLD, 12));
        clearBtn.setPreferredSize(new Dimension(120, 35));
        clearBtn.addActionListener(e -> clearCart());
        
        completeBtn = new JButton("Complete Sale");
        completeBtn.setFont(new Font("Arial", Font.BOLD, 14));
        completeBtn.setBackground(new Color(0, 150, 0));
        completeBtn.setForeground(Color.BLACK);
        completeBtn.setFocusPainted(false);
        completeBtn.setPreferredSize(new Dimension(160, 40));
        completeBtn.setBorder(BorderFactory.createRaisedBevelBorder());
        completeBtn.addActionListener(e -> completeSale());
        
        buttonPanel.add(clearBtn);
        buttonPanel.add(completeBtn);
        
        rightPanel.add(buttonPanel, BorderLayout.NORTH);
        
        // Status
        statusLabel = new JLabel("Ready - Scan a product");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setForeground(new Color(100, 100, 100));
        rightPanel.add(statusLabel, BorderLayout.SOUTH);
        
        bottomAreaPanel.add(rightPanel, BorderLayout.EAST);
        
        tablePanel.add(bottomAreaPanel, BorderLayout.SOUTH);
        
        centerPanel.add(tablePanel, BorderLayout.CENTER);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private void processBarcode(String barcode) {
        if (barcode.isEmpty()) {
            statusLabel.setText("Please enter or scan a barcode");
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, name, selling_price, quantity FROM products WHERE barcode = ? AND is_active = 1")) {
            
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                statusLabel.setText("Product not found: " + barcode);
                return;
            }
            
            int productId = rs.getInt("id");
            String name = rs.getString("name");
            double price = rs.getDouble("selling_price");
            int availableQty = rs.getInt("quantity");
            
            if (availableQty <= 0) {
                statusLabel.setText("Product out of stock: " + name);
                return;
            }
            
            cartManager.addItem(productId, name, price, 1);
            updateCartDisplay();
            statusLabel.setText("Added: " + name);
            barcodeField.setText("");
            
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setQuantityForSelectedItem() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow < 0) {
            statusLabel.setText("Please select an item first");
            return;
        }
        
        String qtyStr = JOptionPane.showInputDialog(this, "Enter new quantity:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            return;
        }
        
        try {
            int newQty = Integer.parseInt(qtyStr.trim());
            if (newQty < 1) {
                statusLabel.setText("Quantity must be at least 1");
                return;
            }
            
            int productId = cartManager.getItemAt(selectedRow).getProductId();
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT quantity FROM products WHERE id = ?")) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("quantity") < newQty) {
                    statusLabel.setText("Not enough stock. Available: " + rs.getInt("quantity"));
                    return;
                }
            }
            
            cartManager.updateQuantity(selectedRow, newQty);
            updateCartDisplay();
            statusLabel.setText("Quantity updated to " + newQty);
            
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid number");
        } catch (SQLException e) {
            statusLabel.setText("Error checking stock: " + e.getMessage());
        }
    }
    
    private void updateCartDisplay() {
        cartModel.setRowCount(0);
        for (int i = 0; i < cartManager.getItemCount(); i++) {
            CartManager.CartItem item = cartManager.getItemAt(i);
            cartModel.addRow(new Object[]{
                item.getQuantity(),
                item.getProductName(),
                currencyFormat.format(item.getUnitPrice()),
                currencyFormat.format(item.getSubtotal())
            });
        }
        
        subtotalLabel.setText("Subtotal: " + currencyFormat.format(cartManager.getSubtotal()));
        vatLabel.setText("VAT (15%): " + currencyFormat.format(cartManager.getVatAmount()));
        totalLabel.setText("TOTAL: " + currencyFormat.format(cartManager.getTotal()));
        
        // ✅ FIX: Always show totals panel, even when empty
        totalsPanel.setVisible(true);
    }
    
    private void clearCart() {
        if (cartManager.getItemCount() > 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Clear all items from cart?",
                "Confirm",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                cartManager.clearCart();
                cartModel.setRowCount(0);
                
                // ✅ FIX: Reset totals to R0.00
                subtotalLabel.setText("Subtotal: R 0.00");
                vatLabel.setText("VAT (15%): R 0.00");
                totalLabel.setText("TOTAL: R 0.00");
                
                totalsPanel.setVisible(true);  // ✅ Always show totals
                statusLabel.setText("Cart cleared");
                barcodeField.requestFocusInWindow();
            }
        } else {
            statusLabel.setText("Cart is already empty");
        }
    }
    
    private void completeSale() {
    System.out.println("========================================");
    System.out.println("🔴 COMPLETE SALE BUTTON CLICKED!");
    System.out.println("========================================");
    System.out.println("Items in cart: " + cartManager.getItemCount());
    
    if (cartManager.getItemCount() == 0) {
        System.out.println("❌ Cart is empty!");
        statusLabel.setText("Cart is empty. Add items first.");
        JOptionPane.showMessageDialog(this,
            "Cart is empty!\n\nPlease add items before completing a sale.",
            "Empty Cart",
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Customer dialog
    String customerName = JOptionPane.showInputDialog(this,
        "Enter customer name (optional):",
        "Customer Details",
        JOptionPane.QUESTION_MESSAGE);
    
    if (customerName == null) {
        statusLabel.setText("Sale cancelled");
        return;
    }
    
    // Payment dialog
    String[] paymentOptions = {"cash", "card", "eft"};
    String paymentMethod = (String) JOptionPane.showInputDialog(this,
        "Select payment method:",
        "Payment",
        JOptionPane.QUESTION_MESSAGE,
        null,
        paymentOptions,
        "cash");
    
    if (paymentMethod == null) {
        statusLabel.setText("Sale cancelled");
        return;
    }
    
    System.out.println("📌 Customer: " + customerName);
    System.out.println("📌 Payment: " + paymentMethod);
    
    statusLabel.setText("Processing sale...");
    
    try {
        System.out.println("📌 Calling saleProcessor.processSale()...");
        boolean success = saleProcessor.processSale(
            SessionManager.getInstance().getUserId(),
            customerName,
            cartManager,
            paymentMethod
        );
        
        if (success) {
            System.out.println("✅ Sale successful! Clearing cart...");
            cartManager.clearCart();
            cartModel.setRowCount(0);
            subtotalLabel.setText("Subtotal: R 0.00");
            vatLabel.setText("VAT (15%): R 0.00");
            totalLabel.setText("TOTAL: R 0.00");
            statusLabel.setText("✅ Sale completed!");
            
            // Ask to view invoice
            int option = JOptionPane.showOptionDialog(this,
                "✅ SALE COMPLETED SUCCESSFULLY!\n\n" +
                "Do you want to view the invoice details?",
                "Sale Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"View Invoice", "Close"},
                "View Invoice");
            
            if (option == JOptionPane.YES_OPTION) {
                saleProcessor.viewLastInvoice();
            }
        } else {
            System.out.println("❌ Sale failed!");
            statusLabel.setText("❌ Sale failed");
        }
    } catch (Exception e) {
        System.out.println("❌ ERROR in completeSale: " + e.getMessage());
        e.printStackTrace();
        statusLabel.setText("❌ Error: " + e.getMessage());
        JOptionPane.showMessageDialog(this,
            "❌ ERROR:\n\n" + e.getMessage() + "\n\n" +
            "Check the console for details.",
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    barcodeField.requestFocusInWindow();
}
    
    private void setupKeyBindings() {
        InputMap inputMap = barcodeField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = barcodeField.getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "clearCart");
        actionMap.put("clearCart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCart();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "completeSale");
        actionMap.put("completeSale", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completeSale();
            }
        });
    }
    
    private void setupSessionTimeout() {
        Timer sessionTimer = new Timer(60000, e -> {
            int timeoutMinutes = ConfigManager.getIntProperty("session.timeout.minutes", 15);
            if (SessionManager.getInstance().isSessionTimedOut(timeoutMinutes)) {
                JOptionPane.showMessageDialog(this,
                    "Session timed out due to inactivity.",
                    "Session Expired",
                    JOptionPane.WARNING_MESSAGE);
                logout();
            }
        });
        sessionTimer.start();
    }
    
    private void logout() {
        SessionManager.getInstance().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}