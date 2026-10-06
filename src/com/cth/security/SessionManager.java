package com.cth.security;


import com.cth.utils.AuditLogger;

import java.util.Date;

public class SessionManager {
    private static SessionManager instance;
    private int userId;
    private String username;
    private String fullName;
    private String role;
    private Date loginTime;
    private long lastActivityTime;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void login(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.loginTime = new Date();
        this.lastActivityTime = System.currentTimeMillis();
        AuditLogger.log(userId, username, "User Login", "User logged in successfully");
    }
    
    public void logout() {
        if (isLoggedIn()) {
            AuditLogger.log(userId, username, "User Logout", "User logged out");
        }
        reset();
    }
    
    private void reset() {
        this.userId = 0;
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.loginTime = null;
        this.lastActivityTime = 0;
    }
    
    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    public boolean isLoggedIn() {
        return userId > 0 && username != null;
    }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    public boolean isCashier() {
        return "cashier".equals(role);
    }
    
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public Date getLoginTime() { return loginTime; }
    public long getSessionDuration() {
        return loginTime != null ? System.currentTimeMillis() - loginTime.getTime() : 0;
    }
    
    public boolean isSessionTimedOut(int timeoutMinutes) {
        if (lastActivityTime == 0) return true;
        long inactiveTime = System.currentTimeMillis() - lastActivityTime;
        return inactiveTime > (timeoutMinutes * 60 * 1000);
    }
}