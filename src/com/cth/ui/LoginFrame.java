package com.cth.ui;


import com.cth.database.DatabaseConnection;
import com.cth.security.PasswordUtil;
import com.cth.security.SessionManager;
import com.cth.utils.AuditLogger;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private int failedAttempts = 0;
    
    public LoginFrame() {
        setTitle("CTH Tools - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initUI();
    }
    
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header
        JLabel titleLabel = new JLabel("CTH Tools Inventory System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        formPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);
        
        // Status label
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        formPanel.add(statusLabel, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginBtn = new JButton("Login");
        JButton exitBtn = new JButton("Exit");
        
        loginBtn.addActionListener(e -> authenticate());
        exitBtn.addActionListener(e -> System.exit(0));
        
        // Enter key presses login
        getRootPane().setDefaultButton(loginBtn);
        
        buttonPanel.add(loginBtn);
        buttonPanel.add(exitBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void authenticate() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            return;
        }
        
        // Check if account is locked
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM users WHERE username = ? AND is_active = 1")) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                statusLabel.setText("Invalid username or password");
                AuditLogger.log("UNKNOWN", "Login Attempt", "Failed login for username: " + username);
                return;
            }
            
            // Check if locked
            Timestamp lockedUntil = rs.getTimestamp("locked_until");
            if (lockedUntil != null && lockedUntil.after(new java.util.Date())) {
                statusLabel.setText("Account is locked. Try again later.");
                return;
            }
            
            // Verify password
            String storedHash = rs.getString("password_hash");
            if (PasswordUtil.verifyPassword(password, storedHash)) {
                // Successful login
                int userId = rs.getInt("id");
                String fullName = rs.getString("full_name");
                String role = rs.getString("role");
                
                // Reset failed attempts
                try (PreparedStatement updateStmt = conn.prepareStatement(
                     "UPDATE users SET failed_attempts = 0, locked_until = NULL, last_login = CURRENT_TIMESTAMP WHERE id = ?")) {
                    updateStmt.setInt(1, userId);
                    updateStmt.executeUpdate();
                }
                
                // Create session
                SessionManager.getInstance().login(userId, username, fullName, role);
                AuditLogger.log(userId, username, "Login", "User logged in successfully from IP: " + getLocalIP());
                
                // Launch appropriate dashboard
                dispose();
                if ("admin".equals(role)) {
                    new AdminDashboard().setVisible(true);
                } else {
                    new POSFrame().setVisible(true);
                }
            } else {
                // Failed attempt
                failedAttempts++;
                int attempts = rs.getInt("failed_attempts") + 1;
                statusLabel.setText("Invalid password. Attempt " + attempts + "/5");
                
                if (attempts >= 5) {
                    // Lock account for 15 minutes
                    try (PreparedStatement lockStmt = conn.prepareStatement(
                         "UPDATE users SET locked_until = datetime('now', '+15 minutes'), failed_attempts = ? WHERE id = ?")) {
                        lockStmt.setInt(1, attempts);
                        lockStmt.setInt(2, rs.getInt("id"));
                        lockStmt.executeUpdate();
                    }
                    statusLabel.setText("Account locked for 15 minutes due to too many failed attempts.");
                } else {
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                         "UPDATE users SET failed_attempts = ? WHERE id = ?")) {
                        updateStmt.setInt(1, attempts);
                        updateStmt.setInt(2, rs.getInt("id"));
                        updateStmt.executeUpdate();
                    }
                }
                
                AuditLogger.log(rs.getInt("id"), username, "Login Attempt", "Failed login attempt");
            }
            
        } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}