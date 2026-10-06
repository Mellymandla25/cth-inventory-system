package com.cth.models;


import java.sql.Timestamp;

public class AuditLog {
    private int id;
    private Integer userId;
    private String username;
    private String action;
    private String details;
    private Timestamp timestamp;
    private String ipAddress;
    
    // Constructors
    public AuditLog() {}
    
    public AuditLog(String username, String action, String details) {
        this.username = username;
        this.action = action;
        this.details = details;
    }
    
    public AuditLog(Integer userId, String username, String action, String details) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    @Override
    public String toString() {
        return timestamp + " - " + username + " - " + action + " - " + details;
    }
}