package com.cth.ui;

import com.cth.database.DatabaseConnection;
import com.cth.security.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminDashboardPanel extends JPanel {
    private DecimalFormat currencyFormat = new DecimalFormat("R #,##0.00");
    private JLabel salesTodayLabel;
    private JLabel salesMonthLabel;
    private JLabel totalProductsLabel;
    private JLabel lowStockLabel;
    private JLabel totalRevenueLabel;
    private JLabel cashiersLabel;
    private JTextArea recentSalesArea;
    private JTextArea lowStockArea;
    
    public AdminDashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        initUI();
        loadDashboardData();
    }
    
    private void initUI() {
        // Title
        JLabel titleLabel = new JLabel("📊 ADMIN DASHBOARD");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);
        
        // Main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 0: Sales Today & Sales This Month
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.5;
        mainPanel.add(createStatCard("💰 Sales Today", "R 0.00", new Color(33, 150, 243)), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        mainPanel.add(createStatCard("📈 Sales This Month", "R 0.00", new Color(76, 175, 80)), gbc);
        
        // Row 1: Total Products & Low Stock
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(createStatCard("📦 Total Products", "0", new Color(255, 152, 0)), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        mainPanel.add(createStatCard("⚠️ Low Stock Items", "0", new Color(244, 67, 54)), gbc);
        
        // Row 2: Total Revenue & Active Cashiers
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(createStatCard("💵 Total Revenue", "R 0.00", new Color(156, 39, 176)), gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        mainPanel.add(createStatCard("👤 Active Cashiers", "0", new Color(0, 188, 212)), gbc);
        
        // Row 3: Recent Sales (left) & Low Stock List (right)
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(createRecentSalesPanel(), gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        mainPanel.add(createLowStockPanel(), gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titleLabel.setForeground(Color.GRAY);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(color);
        
        // Store reference to update later
        if (title.equals("💰 Sales Today")) {
            salesTodayLabel = valueLabel;
        } else if (title.equals("📈 Sales This Month")) {
            salesMonthLabel = valueLabel;
        } else if (title.equals("📦 Total Products")) {
            totalProductsLabel = valueLabel;
        } else if (title.equals("⚠️ Low Stock Items")) {
            lowStockLabel = valueLabel;
        } else if (title.equals("💵 Total Revenue")) {
            totalRevenueLabel = valueLabel;
        } else if (title.equals("👤 Active Cashiers")) {
            cashiersLabel = valueLabel;
        }
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRecentSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("🕐 Recent Sales"));
        panel.setPreferredSize(new Dimension(0, 200));
        
        recentSalesArea = new JTextArea();
        recentSalesArea.setEditable(false);
        recentSalesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        recentSalesArea.setBackground(new Color(248, 248, 248));
        
        JScrollPane scrollPane = new JScrollPane(recentSalesArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("⚠️ Low Stock Alert"));
        panel.setPreferredSize(new Dimension(0, 200));
        
        lowStockArea = new JTextArea();
        lowStockArea.setEditable(false);
        lowStockArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lowStockArea.setBackground(new Color(255, 240, 240));
        lowStockArea.setForeground(Color.RED);
        
        JScrollPane scrollPane = new JScrollPane(lowStockArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadDashboardData() {
        SwingUtilities.invokeLater(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Get today's date
                LocalDate today = LocalDate.now();
                String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String monthStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                
                // 1. Sales Today
                String salesTodaySQL = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE date(sale_date) = ? AND voided = 0";
                try (PreparedStatement pstmt = conn.prepareStatement(salesTodaySQL)) {
                    pstmt.setString(1, todayStr);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        salesTodayLabel.setText(currencyFormat.format(rs.getDouble(1)));
                    }
                }
                
                // 2. Sales This Month
                String salesMonthSQL = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE strftime('%Y-%m', sale_date) = ? AND voided = 0";
                try (PreparedStatement pstmt = conn.prepareStatement(salesMonthSQL)) {
                    pstmt.setString(1, monthStr);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        salesMonthLabel.setText(currencyFormat.format(rs.getDouble(1)));
                    }
                }
                
                // 3. Total Products
                String productsSQL = "SELECT COUNT(*) FROM products WHERE is_active = 1";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(productsSQL);
                    if (rs.next()) {
                        totalProductsLabel.setText(String.valueOf(rs.getInt(1)));
                    }
                }
                
                // 4. Low Stock Items
                String lowStockSQL = "SELECT COUNT(*) FROM products WHERE quantity <= low_stock_threshold AND is_active = 1";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(lowStockSQL);
                    if (rs.next()) {
                        lowStockLabel.setText(String.valueOf(rs.getInt(1)));
                    }
                }
                
                // 5. Total Revenue
                String revenueSQL = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE voided = 0";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(revenueSQL);
                    if (rs.next()) {
                        totalRevenueLabel.setText(currencyFormat.format(rs.getDouble(1)));
                    }
                }
                
                // 6. Active Cashiers
                String cashiersSQL = "SELECT COUNT(*) FROM users WHERE role = 'cashier' AND is_active = 1";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(cashiersSQL);
                    if (rs.next()) {
                        cashiersLabel.setText(String.valueOf(rs.getInt(1)));
                    }
                }
                
                // 7. Recent Sales (Last 5)
                String recentSalesSQL = "SELECT invoice_number, customer_name, total_amount, sale_date FROM sales WHERE voided = 0 ORDER BY sale_date DESC LIMIT 5";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(recentSalesSQL);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invoice #           Customer          Amount       Date\n");
                    sb.append("----------------------------------------------------\n");
                    while (rs.next()) {
                        String customer = rs.getString("customer_name");
                        if (customer == null || customer.isEmpty()) customer = "Walk-in";
                        sb.append(String.format("%-16s  %-16s  %10s  %s\n",
                            rs.getString("invoice_number"),
                            customer,
                            currencyFormat.format(rs.getDouble("total_amount")),
                            rs.getTimestamp("sale_date").toString().substring(0, 10)
                        ));
                    }
                    recentSalesArea.setText(sb.toString());
                }
                
                // 8. Low Stock Products List
                String lowStockListSQL = "SELECT name, quantity, low_stock_threshold FROM products WHERE quantity <= low_stock_threshold AND is_active = 1 ORDER BY quantity ASC LIMIT 10";
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery(lowStockListSQL);
                    StringBuilder sb = new StringBuilder();
                    if (!rs.next()) {
                        sb.append("✅ All products are well stocked!");
                    } else {
                        sb.append("Product Name                    Qty    Threshold\n");
                        sb.append("---------------------------------------------\n");
                        // Need to process first result and continue
                        do {
                            sb.append(String.format("%-30s  %3d    %3d\n",
                                rs.getString("name"),
                                rs.getInt("quantity"),
                                rs.getInt("low_stock_threshold")
                            ));
                        } while (rs.next());
                    }
                    lowStockArea.setText(sb.toString());
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error loading dashboard: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    public void refresh() {
        loadDashboardData();
    }
}