package com.cth.security;

import com.cth.config.ConfigManager;
import com.cth.utils.AuditLogger;

import javax.swing.*;
import java.awt.*;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Base64;

public class LicenseManager {
    
    private static boolean licenseActivated = false;
    
    public static boolean verifyLicense() {
        String storedKey = ConfigManager.getProperty("license.key", "");
        if (storedKey.isEmpty()) {
            return false;
        }
        
        String expectedKey = generateLicenseKey();
        return storedKey.equals(expectedKey) || storedKey.equals("CTH-DEMO-2024");
    }
    
    public static boolean showLicenseActivation() {
        JDialog dialog = new JDialog((Frame) null, "License Activation", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 250);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Installation ID
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Installation ID:"), gbc);
        gbc.gridx = 1;
        String installId = ConfigManager.getProperty("installation.id", "UNKNOWN");
        JTextField installField = new JTextField(installId, 20);
        installField.setEditable(false);
        panel.add(installField, gbc);
        
        // License key input
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("License Key:"), gbc);
        gbc.gridx = 1;
        JTextField keyField = new JTextField(20);
        keyField.setToolTipText("Enter your license key or use CTH-DEMO-2024");
        panel.add(keyField, gbc);
        
        // Demo info
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel demoLabel = new JLabel("Demo Key: CTH-DEMO-2024");
        demoLabel.setForeground(Color.BLUE);
        demoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        panel.add(demoLabel, gbc);
        
        dialog.add(panel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton activateBtn = new JButton("Activate");
        JButton exitBtn = new JButton("Exit");
        
        activateBtn.addActionListener(e -> {
            String enteredKey = keyField.getText().trim();
            if (enteredKey.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                    "Please enter a license key.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Accept demo key or generated key
            if (enteredKey.equals("CTH-DEMO-2024") || enteredKey.equals(generateLicenseKey())) {
                ConfigManager.setProperty("license.key", enteredKey);
                AuditLogger.log("SYSTEM", "License Activation", "License activated successfully");
                JOptionPane.showMessageDialog(dialog, 
                    "✅ License activated successfully!\n\nThe application will now start.", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                licenseActivated = true;
                dialog.dispose(); // Close the dialog
                System.out.println("✅ License activated, dialog closed. Login screen should appear now.");
            } else {
                JOptionPane.showMessageDialog(dialog, 
                    "❌ Invalid license key.\n\nPlease check your key and try again.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        exitBtn.addActionListener(e -> {
            System.exit(0);
        });
        
        buttonPanel.add(activateBtn);
        buttonPanel.add(exitBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
        
        return licenseActivated;
    }
    
    private static String generateLicenseKey() {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(java.net.InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            
            // Combine MAC with a secret salt
            String combined = Base64.getEncoder().encodeToString(mac) + "CTH-SECRET-SALT-2024";
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes());
            
            // Return first 16 characters as license key
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            return "FALLBACK-KEY-2024"; // Fallback for systems without network
        }
    }
}