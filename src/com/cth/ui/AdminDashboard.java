package com.cth.ui;

import com.cth.config.ConfigManager;
import com.cth.database.DatabaseConnection;
import com.cth.security.SessionManager;
import com.cth.utils.AuditLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;

public class AdminDashboard extends JFrame {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private Timer refreshTimer;
    private JTabbedPane tabbedPane;
    private DecimalFormat currencyFormat = new DecimalFormat("R #,##0.00");
    
    public AdminDashboard() {
        setTitle("CTH Tools - Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        
        initUI();
        loadProducts();
        startAutoRefresh();
        setupSessionTimeout();
    }
    
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // TOP PANEL
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("Welcome, " + SessionManager.getInstance().getFullName() + " (Admin)");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> logout());
        rightPanel.add(logoutBtn);
        topPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // TABBED PANE
        tabbedPane = new JTabbedPane();
        
        // --- REMOVED: Dashboard Tab (causing the crash!) ---
        
        // Products Tab
        JPanel productsPanel = createProductsPanel();
        tabbedPane.addTab("📦 Products & Stock", productsPanel);
        
        // Reports Tab
        JPanel reportsPanel = new ReportsPanel();
        tabbedPane.addTab("📊 Reports", reportsPanel);
        
        // User Management Tab
        JPanel usersPanel = createUsersPanel();
        tabbedPane.addTab("👥 User Management", usersPanel);
        
        // Settings Tab
        JPanel settingsPanel = createSettingsPanel();
        tabbedPane.addTab("⚙️ Settings", settingsPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // STATUS BAR
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JButton addBtn = new JButton("Add Product");
        JButton refreshBtn = new JButton("Refresh");
        
        searchBtn.addActionListener(e -> searchProducts(searchField.getText()));
        addBtn.addActionListener(e -> showProductDialog(null));
        refreshBtn.addActionListener(e -> loadProducts());
        
        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField);
        controlPanel.add(searchBtn);
        controlPanel.add(addBtn);
        controlPanel.add(refreshBtn);
        panel.add(controlPanel, BorderLayout.NORTH);
        
        String[] columns = {"ID", "Barcode", "Name", "Category", "Qty", "Cost Price", "Selling Price", "Supplier"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        productTable = new JTable(tableModel);
        productTable.setRowHeight(25);
        
        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    try {
                        int qty = Integer.parseInt(tableModel.getValueAt(row, 4).toString());
                        int threshold = ConfigManager.getIntProperty("low.stock.threshold", 5);
                        if (qty <= threshold) {
                            c.setBackground(Color.RED);
                            c.setForeground(Color.WHITE);
                        } else {
                            c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(240, 240, 240));
                            c.setForeground(Color.BLACK);
                        }
                    } catch (Exception e) {}
                }
                return c;
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Product");
        JMenuItem deleteItem = new JMenuItem("Deactivate Product");
        
        editItem.addActionListener(e -> {
            int row = productTable.getSelectedRow();
            if (row >= 0) {
                int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                showProductDialog(id);
            }
        });
        
        deleteItem.addActionListener(e -> {
            int row = productTable.getSelectedRow();
            if (row >= 0) {
                int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                deactivateProduct(id);
            }
        });
        
        popupMenu.add(editItem);
        popupMenu.add(deleteItem);
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = productTable.rowAtPoint(e.getPoint());
                    productTable.setRowSelectionInterval(row, row);
                    popupMenu.show(productTable, e.getX(), e.getY());
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(productTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadProducts() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, barcode, name, category, quantity, cost_price, selling_price, supplier " +
                 "FROM products WHERE is_active = 1 ORDER BY name")) {
            
            tableModel.setRowCount(0);
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    currencyFormat.format(rs.getDouble("cost_price")),
                    currencyFormat.format(rs.getDouble("selling_price")),
                    rs.getString("supplier")
                });
            }
            statusLabel.setText("Loaded " + tableModel.getRowCount() + " products");
            
        } catch (SQLException e) {
            statusLabel.setText("Error loading products: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void searchProducts(String query) {
        if (query.trim().isEmpty()) {
            loadProducts();
            return;
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, barcode, name, category, quantity, cost_price, selling_price, supplier " +
                 "FROM products WHERE is_active = 1 AND (name LIKE ? OR barcode LIKE ? OR category LIKE ?) " +
                 "ORDER BY name")) {
            
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            tableModel.setRowCount(0);
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    currencyFormat.format(rs.getDouble("cost_price")),
                    currencyFormat.format(rs.getDouble("selling_price")),
                    rs.getString("supplier")
                });
            }
            statusLabel.setText("Found " + tableModel.getRowCount() + " products matching '" + query + "'");
            
        } catch (SQLException e) {
            statusLabel.setText("Error searching: " + e.getMessage());
        }
    }
    
    private void showProductDialog(Integer productId) {
        ProductManagementDialog dialog = new ProductManagementDialog(this, productId);
        dialog.setVisible(true);
        if (dialog.isProductSaved()) {
            loadProducts();
        }
    }
    
    private void deactivateProduct(int id) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to deactivate this product?",
            "Confirm Deactivation",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE products SET is_active = 0 WHERE id = ?")) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
                
                AuditLogger.log(SessionManager.getInstance().getUserId(),
                    SessionManager.getInstance().getUsername(),
                    "Product Deactivated",
                    "Deactivated product ID: " + id);
                
                loadProducts();
                statusLabel.setText("Product deactivated successfully");
                
            } catch (SQLException e) {
                statusLabel.setText("Error deactivating product: " + e.getMessage());
            }
        }
    }
    
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] columns = {"ID", "Username", "Full Name", "Role", "Status", "Last Login"};
        DefaultTableModel userModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable userTable = new JTable(userModel);
        JScrollPane scrollPane = new JScrollPane(userTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addUserBtn = new JButton("Add User");
        JButton resetPasswordBtn = new JButton("Reset Password");
        JButton toggleStatusBtn = new JButton("Toggle Status");
        
        addUserBtn.addActionListener(e -> {
            String username = JOptionPane.showInputDialog(this, "Enter username:");
            if (username != null && !username.trim().isEmpty()) {
                String fullName = JOptionPane.showInputDialog(this, "Enter full name:");
                if (fullName != null && !fullName.trim().isEmpty()) {
                    String role = (String) JOptionPane.showInputDialog(this,
                        "Select role:",
                        "Role",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{"cashier", "admin"},
                        "cashier");
                    if (role != null) {
                        createUser(username.trim(), fullName.trim(), role);
                        loadUsers(userModel);
                    }
                }
            }
        });
        
        resetPasswordBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                int id = Integer.parseInt(userModel.getValueAt(row, 0).toString());
                String username = userModel.getValueAt(row, 1).toString();
                resetUserPassword(id, username);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a user first.");
            }
        });
        
        toggleStatusBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row >= 0) {
                int id = Integer.parseInt(userModel.getValueAt(row, 0).toString());
                String currentStatus = userModel.getValueAt(row, 4).toString();
                toggleUserStatus(id, "Active".equals(currentStatus));
                loadUsers(userModel);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a user first.");
            }
        });
        
        buttonPanel.add(addUserBtn);
        buttonPanel.add(resetPasswordBtn);
        buttonPanel.add(toggleStatusBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        loadUsers(userModel);
        return panel;
    }
    
    private void loadUsers(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, username, full_name, role, is_active, last_login FROM users ORDER BY id")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                    rs.getTimestamp("last_login")
                });
            }
        } catch (SQLException e) {
            statusLabel.setText("Error loading users: " + e.getMessage());
        }
    }
    
    private void createUser(String username, String fullName, String role) {
        String tempPassword = com.cth.security.PasswordUtil.generateRandomPassword(8);
        String passwordHash = com.cth.security.PasswordUtil.hashPassword(tempPassword);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            
            AuditLogger.log(SessionManager.getInstance().getUserId(),
                SessionManager.getInstance().getUsername(),
                "User Created",
                "Created user: " + username + " with role: " + role);
            
            JOptionPane.showMessageDialog(this,
                "User created successfully!\n\nUsername: " + username + "\nTemporary Password: " + tempPassword,
                "User Created",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
            } else {
                statusLabel.setText("Error creating user: " + e.getMessage());
            }
        }
    }
    
    private void resetUserPassword(int userId, String username) {
        String newPassword = com.cth.security.PasswordUtil.generateRandomPassword(8);
        String passwordHash = com.cth.security.PasswordUtil.hashPassword(newPassword);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE users SET password_hash = ? WHERE id = ?")) {
            pstmt.setString(1, passwordHash);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            
            AuditLogger.log(SessionManager.getInstance().getUserId(),
                SessionManager.getInstance().getUsername(),
                "Password Reset",
                "Reset password for user: " + username);
            
            JOptionPane.showMessageDialog(this,
                "Password reset for " + username + "\n\nNew Password: " + newPassword,
                "Password Reset",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            statusLabel.setText("Error resetting password: " + e.getMessage());
        }
    }
    
    private void toggleUserStatus(int userId, boolean currentlyActive) {
        int newStatus = currentlyActive ? 0 : 1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE users SET is_active = ? WHERE id = ?")) {
            pstmt.setInt(1, newStatus);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            
            AuditLogger.log(SessionManager.getInstance().getUserId(),
                SessionManager.getInstance().getUsername(),
                "User Status Toggled",
                "Toggled user ID: " + userId + " to " + (newStatus == 1 ? "Active" : "Inactive"));
            
        } catch (SQLException e) {
            statusLabel.setText("Error toggling user status: " + e.getMessage());
        }
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        panel.add(new JLabel("Business Name:"), gbc);
        gbc.gridy++;
        JTextField businessNameField = new JTextField(ConfigManager.getProperty("app.name", "CTH Tools"), 30);
        panel.add(businessNameField, gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("Address:"), gbc);
        gbc.gridy++;
        JTextField addressField = new JTextField(ConfigManager.getProperty("app.address", "Carletonville, South Africa"), 30);
        panel.add(addressField, gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridy++;
        JTextField phoneField = new JTextField(ConfigManager.getProperty("app.phone", "+27 82 123 4567"), 30);
        panel.add(phoneField, gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("Low Stock Threshold:"), gbc);
        gbc.gridy++;
        JTextField thresholdField = new JTextField(String.valueOf(ConfigManager.getIntProperty("low.stock.threshold", 5)), 30);
        panel.add(thresholdField, gbc);
        
        gbc.gridy++;
        JButton saveBtn = new JButton("Save Settings");
        panel.add(saveBtn, gbc);
        
        saveBtn.addActionListener(e -> {
            ConfigManager.setProperty("app.name", businessNameField.getText());
            ConfigManager.setProperty("app.address", addressField.getText());
            ConfigManager.setProperty("app.phone", phoneField.getText());
            ConfigManager.setProperty("low.stock.threshold", thresholdField.getText());
            JOptionPane.showMessageDialog(this, "Settings saved successfully!");
            AuditLogger.log(SessionManager.getInstance().getUserId(),
                SessionManager.getInstance().getUsername(),
                "Settings Updated",
                "Application settings were updated");
        });
        
        return panel;
    }
    
    private void startAutoRefresh() {
        refreshTimer = new Timer(30000, e -> loadProducts());
        refreshTimer.start();
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
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        SessionManager.getInstance().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
    
    @Override
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        super.dispose();
    }
}