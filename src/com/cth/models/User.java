package com.cth.models;


import java.sql.Timestamp;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private int failedAttempts;
    private Timestamp lockedUntil;
    
    // Constructors
    public User() {}
    
    public User(int id, String username, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    
    public Timestamp getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Timestamp lockedUntil) { this.lockedUntil = lockedUntil; }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    public boolean isCashier() {
        return "cashier".equals(role);
    }
    
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.after(new Timestamp(System.currentTimeMillis()));
    }
    
    @Override
    public String toString() {
        return fullName + " (" + username + ")";
    }
}