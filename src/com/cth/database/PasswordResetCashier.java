package com.cth.database;

import com.cth.security.PasswordUtil;
import java.sql.*;

public class PasswordResetCashier {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Reset cashier password to "cashier123"
            String newPassword = "cashier123";
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            
            String sql = "UPDATE users SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE username = 'cashier'";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, hashedPassword);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Cashier password reset successfully!");
                System.out.println("New password: " + newPassword);
            } else {
                System.out.println("❌ Cashier user not found!");
            }
            
            pstmt.close();
            conn.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}