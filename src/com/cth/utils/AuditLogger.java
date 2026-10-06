package com.cth.utils;


import com.cth.ui.*;
import com.cth.database.DatabaseConnection;
import com.cth.security.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class AuditLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE = PathUtils.getLogPath() + "/audit.log";
    
    public static void log(String username, String action, String details) {
        log(0, username, action, details);
    }
    
    public static void log(int userId, String username, String action, String details) {
        // Log to database
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO audit_log (user_id, username, action, details) VALUES (?, ?, ?, ?)")) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, username);
            pstmt.setString(3, action);
            pstmt.setString(4, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Fallback to file logging
            logToFile(username, action, details);
        }
        
        // Also log to file for redundancy
        logToFile(username, action, details);
    }
    
    private static void logToFile(String username, String action, String details) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String sessionInfo = "";
            if (SessionManager.getInstance().isLoggedIn()) {
                sessionInfo = " [User: " + SessionManager.getInstance().getUsername() + "]";
            }
            pw.println(timestamp + sessionInfo + " - " + username + " - " + action + " - " + details);
        } catch (IOException e) {
            // Silent fail - we've already tried database
        }
    }

    public static void log(String system, String application_shutting_down__Performing_bac) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}