package com.cth.database;


import com.cth.config.ConfigManager;
import com.cth.utils.PathUtils;

import java.sql.*;
import java.io.File;

public class DatabaseConnection {
    private static Connection connection = null;
    private static final String DB_PATH = "cth_tools.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    
    private DatabaseConnection() {} // Prevent instantiation
    
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            // Enable foreign key constraints
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;"); // Better concurrency
                stmt.execute("PRAGMA synchronous = NORMAL;"); // Performance with safety
            }
        }
        return connection;
    }
    
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void performBackup() {
        try {
            String backupPath = PathUtils.getBackupPath() + 
                               File.separator + 
                               "backup_" + 
                               new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date()) + 
                               ".sql";
            
            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                return;
            }
            
            // Use SQLite's backup API via JDBC
            Connection backupConn = DriverManager.getConnection("jdbc:sqlite:" + backupPath);
            
            try (Statement stmt = backupConn.createStatement()) {
                // Create backup using SQLite's backup command
                stmt.execute("BEGIN IMMEDIATE;");
                
                // Backup all tables
                String[] tables = {"users", "products", "sales", "sale_items", "audit_log"};
                for (String table : tables) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS backup_" + table + " AS SELECT * FROM " + table + ";");
                }
                
                stmt.execute("COMMIT;");
            }
            
            backupConn.close();
            
            // Delete old backups (retain only last 7 days)
            File backupDir = new File(PathUtils.getBackupPath());
            File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".sql"));
            if (backups != null && backups.length > 7) {
                java.util.Arrays.sort(backups, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (int i = 7; i < backups.length; i++) {
                    backups[i].delete();
                }
            }
            
        } catch (SQLException e) {
            // Log but don't crash - backup is best-effort
            System.err.println("Backup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static boolean checkIntegrity() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA integrity_check;");
            if (rs.next()) {
                String result = rs.getString(1);
                return "ok".equals(result);
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }
}