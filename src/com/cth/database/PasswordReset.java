package com.cth.database;

import com.cth.security.PasswordUtil;
import java.sql.*;

public class PasswordReset {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Reset admin password to "admin123"
            String newPassword = "admin123";
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            
            String sql = "UPDATE users SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE username = 'admin'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hashedPassword);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Admin password reset successfully!");
                System.out.println("New password: " + newPassword);
            } else {
                System.out.println("❌ Admin user not found!");
            }
            
            pstmt.close();
            conn.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}