package com.cth.database;


import com.cth.utils.PathUtils;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatabaseBackup {
    
    private static final String DB_PATH = "cth_tools.db";
    
    public static void performBackup() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String backupPath = PathUtils.getBackupPath() + File.separator + "backup_" + timestamp + ".sql";
            
            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                System.out.println("Database file not found. Skipping backup.");
                return;
            }
            
            // Use SQLite's backup API via JDBC
            try (Connection conn = DatabaseConnection.getConnection();
                 Connection backupConn = DriverManager.getConnection("jdbc:sqlite:" + backupPath);
                 Statement stmt = conn.createStatement();
                 Statement backupStmt = backupConn.createStatement()) {
                
                // Begin transaction
                backupStmt.execute("BEGIN IMMEDIATE;");
                
                // Get all table names
                ResultSet tables = conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    // Skip SQLite internal tables
                    if (tableName.startsWith("sqlite_")) continue;
                    
                    // Create table schema
                    ResultSet schema = stmt.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
                    if (schema.next()) {
                        String createSQL = schema.getString("sql");
                        backupStmt.execute(createSQL);
                    }
                    
                    // Copy data
                    ResultSet data = stmt.executeQuery("SELECT * FROM " + tableName);
                    ResultSetMetaData meta = data.getMetaData();
                    int columnCount = meta.getColumnCount();
                    
                    while (data.next()) {
                        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
                        for (int i = 1; i <= columnCount; i++) {
                            String value = data.getString(i);
                            if (value == null) {
                                insertSQL.append("NULL");
                            } else {
                                insertSQL.append("'").append(value.replace("'", "''")).append("'");
                            }
                            if (i < columnCount) insertSQL.append(", ");
                        }
                        insertSQL.append(")");
                        backupStmt.execute(insertSQL.toString());
                    }
                }
                
                backupStmt.execute("COMMIT;");
                System.out.println("✅ Backup created: " + backupPath);
                
            } catch (SQLException e) {
                System.err.println("❌ Backup failed: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Delete old backups (keep only last 7)
            deleteOldBackups();
            
        } catch (Exception e) {
            System.err.println("❌ Backup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void deleteOldBackups() {
        File backupDir = new File(PathUtils.getBackupPath());
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("backup_") && name.endsWith(".sql"));
        
        if (backups != null && backups.length > 7) {
            java.util.Arrays.sort(backups, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = 7; i < backups.length; i++) {
                if (backups[i].delete()) {
                    System.out.println("🗑️ Deleted old backup: " + backups[i].getName());
                }
            }
        }
    }
    
    public static boolean restoreBackup(File backupFile) {
        if (!backupFile.exists() || !backupFile.getName().endsWith(".sql")) {
            return false;
        }
        
        try {
            // Close existing connection
            DatabaseConnection.closeConnection();
            
            // Delete current database
            File dbFile = new File(DB_PATH);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            
            // Execute the backup SQL
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sql.append(line);
                    if (line.endsWith(";")) {
                        stmt.execute(sql.toString());
                        sql = new StringBuilder();
                    }
                }
                
                System.out.println("✅ Restore successful from: " + backupFile.getName());
                return true;
                
            } catch (SQLException | IOException e) {
                System.err.println("❌ Restore failed: " + e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Restore failed: " + e.getMessage());
            return false;
        }
    }
}