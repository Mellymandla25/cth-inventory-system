package com.cth.ui;


import com.cth.database.DatabaseConnection;
import com.cth.security.PasswordUtil;
import com.cth.security.SessionManager;
import com.cth.utils.AuditLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementDialog extends JDialog {
    private DefaultTableModel userTableModel;
    private JTable userTable;
    private JLabel statusLabel;
    
    public UserManagementDialog(Frame owner) {
        super(owner, "User Management", true);
        setSize(700, 500);
        setLocationRelativeTo(owner);
        initUI();
        loadUsers();
    }
    
    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        
        // Table
        String[] columns = {"ID", "Username", "Full Name", "Role", "Status", "Last Login"};
        userTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        userTable = new JTable(userTableModel);
        userTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(userTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addBtn = new JButton("Add User");
        JButton editBtn = new JButton("Edit User");
        JButton resetBtn = new JButton("Reset Password");
        JButton toggleBtn = new JButton("Toggle Status");
        JButton refreshBtn = new JButton("Refresh");
        JButton closeBtn = new JButton("Close");
        
        addBtn.addActionListener(e -> showAddUserDialog());
        editBtn.addActionListener(e -> showEditUserDialog());
        resetBtn.addActionListener(e -> resetPassword());
        toggleBtn.addActionListener(e -> toggleUserStatus());
        refreshBtn.addActionListener(e -> loadUsers());
        closeBtn.addActionListener(e -> dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(resetBtn);
        buttonPanel.add(toggleBtn);
        buttonPanel.add(refreshBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Status
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        add(statusLabel, BorderLayout.NORTH);
    }
    
    private void loadUsers() {
        userTableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, username, full_name, role, is_active, last_login " +
                 "FROM users ORDER BY id")) {
            
            while (rs.next()) {
                userTableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                    rs.getTimestamp("last_login") != null ? 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(rs.getTimestamp("last_login")) : 
                        "Never"
                });
            }
            
            statusLabel.setText("Loaded " + userTableModel.getRowCount() + " users");
            
        } catch (SQLException e) {
            statusLabel.setText("Error loading users: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add User", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Fields
        JTextField usernameField = new JTextField(20);
        JTextField fullNameField = new JTextField(20);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"cashier", "admin"});
        
        dialog.add(new JLabel("Username:*"), gbc);
        gbc.gridx = 1;
        dialog.add(usernameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        dialog.add(new JLabel("Full Name:*"), gbc);
        gbc.gridx = 1;
        dialog.add(fullNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        dialog.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        dialog.add(roleCombo, gbc);
        
        // Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Create");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || fullName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Username and Full Name are required.");
                return;
            }
            
            createUser(username, fullName, role);
            dialog.dispose();
            loadUsers();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, gbc);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void createUser(String username, String fullName, String role) {
        String tempPassword = PasswordUtil.generateRandomPassword(8);
        String passwordHash = PasswordUtil.hashPassword(tempPassword);
        
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
                "Created user: " + username + " (Role: " + role + ")");
            
            JOptionPane.showMessageDialog(this,
                "User created successfully!\n\n" +
                "Username: " + username + "\n" +
                "Temporary Password: " + tempPassword + "\n\n" +
                "Please provide this to the user and tell them to change it.",
                "User Created",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                JOptionPane.showMessageDialog(this,
                    "Username already exists. Please choose another.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error creating user: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
            e.printStackTrace();
        }
    }
    
    private void showEditUserDialog() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.");
            return;
        }
        
        int userId = (int) userTableModel.getValueAt(selectedRow, 0);
        String currentFullName = (String) userTableModel.getValueAt(selectedRow, 2);
        String currentRole = (String) userTableModel.getValueAt(selectedRow, 3);
        
        JDialog dialog = new JDialog(this, "Edit User", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JTextField fullNameField = new JTextField(currentFullName, 20);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"cashier", "admin"});
        roleCombo.setSelectedItem(currentRole);
        
        dialog.add(new JLabel("Full Name:*"), gbc);
        gbc.gridx = 1;
        dialog.add(fullNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        dialog.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        dialog.add(roleCombo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Update");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String fullName = fullNameField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            
            if (fullName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Full Name is required.");
                return;
            }
            
            updateUser(userId, fullName, role);
            dialog.dispose();
            loadUsers();
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, gbc);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void updateUser(int userId, String fullName, String role) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE users SET full_name = ?, role = ? WHERE id = ?")) {
            
            pstmt.setString(1, fullName);
            pstmt.setString(2, role);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
            
            AuditLogger.log(SessionManager.getInstance().getUserId(),
                SessionManager.getInstance().getUsername(),
                "User Updated",
                "Updated user ID: " + userId);
            
            JOptionPane.showMessageDialog(this, "User updated successfully!");
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error updating user: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void resetPassword() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user.");
            return;
        }
        
        int userId = (int) userTableModel.getValueAt(selectedRow, 0);
        String username = (String) userTableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reset password for user: " + username + "?",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String newPassword = PasswordUtil.generateRandomPassword(8);
            String passwordHash = PasswordUtil.hashPassword(newPassword);
            
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
                    "Password reset for " + username + "\n\nNew Password: " + newPassword + "\n\n" +
                    "Please provide this to the user.",
                    "Password Reset",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Error resetting password: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void toggleUserStatus() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user.");
            return;
        }
        
        int userId = (int) userTableModel.getValueAt(selectedRow, 0);
        String username = (String) userTableModel.getValueAt(selectedRow, 1);
        boolean currentlyActive = "Active".equals(userTableModel.getValueAt(selectedRow, 4));
        
        int newStatus = currentlyActive ? 0 : 1;
        String action = currentlyActive ? "deactivate" : "activate";
        
        int confirm = JOptionPane.showConfirmDialog(this,
            action + " user: " + username + "?",
            "Confirm",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE users SET is_active = ? WHERE id = ?")) {
                
                pstmt.setInt(1, newStatus);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
                
                AuditLogger.log(SessionManager.getInstance().getUserId(),
                    SessionManager.getInstance().getUsername(),
                    "User Status Toggled",
                    action + " user: " + username);
                
                loadUsers();
                JOptionPane.showMessageDialog(this, "User " + action + "d successfully!");
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Error toggling user status: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}