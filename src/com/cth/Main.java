package com.cth;

import com.cth.database.DatabaseConnection;
import com.cth.database.DatabaseInitializer;
import com.cth.security.LicenseManager;
import com.cth.ui.LoginFrame;
import com.cth.utils.AuditLogger;
import com.cth.utils.BackupScheduler;
import com.cth.utils.PathUtils;

import javax.swing.*;
import java.awt.*;

public class Main {
    private static final String APP_NAME = "CTH Tools Inventory System";
    private static final String VERSION = "2.0.0";
    private static LoginFrame loginFrame;
    
    public static void main(String[] args) {
        try {
            // Set Look and Feel to system default
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ex) {
                    // Use default
                }
            }
            
            // 1. Create necessary directories
            PathUtils.createDirectories();
            
            // 2. Initialize database
            DatabaseInitializer.initialize();
            
            // 3. Start backup scheduler (daily at 2 AM)
            BackupScheduler.startScheduler();
            
            // 4. Verify license
            if (!LicenseManager.verifyLicense()) {
                if (!LicenseManager.showLicenseActivation()) {
                    System.out.println("License activation failed. Exiting...");
                    System.exit(0);
                }
            }
            
            // 5. Launch login screen
            SwingUtilities.invokeLater(() -> {
                loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            
            // 6. Register shutdown hook for backup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    AuditLogger.log("SYSTEM", "Application shutting down - Performing backup");
                    DatabaseConnection.performBackup();
                } catch (Exception e) {
                    System.err.println("Backup on shutdown failed: " + e.getMessage());
                }
            }));
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Critical Error: " + e.getMessage() + "\n\n" +
                "Please contact support with the error details.",
                "Startup Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    public static String getAppName() {
        return APP_NAME;
    }
    
    public static String getVersion() {
        return VERSION;
    }
}