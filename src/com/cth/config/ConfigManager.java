package com.cth.config;


import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;
    
    static {
        properties = new Properties();
        loadConfig();
    }
    
    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // Create default config if not exists
            setDefaultConfig();
            saveConfig();
        }
    }
    
    private static void setDefaultConfig() {
        properties.setProperty("db.path", "cth_tools.db");
        properties.setProperty("backup.retention.days", "7");
        properties.setProperty("app.name", "CTH Tools");
        properties.setProperty("app.address", "Carletonville, South Africa");
        properties.setProperty("app.phone", "+27 82 123 4567");
        properties.setProperty("app.bank.name", "First National Bank");
        properties.setProperty("app.bank.account", "1234567890");
        properties.setProperty("app.bank.branch", "123456");
        properties.setProperty("app.bank.account.holder", "CTH Tools");
        properties.setProperty("low.stock.threshold", "5");
        properties.setProperty("session.timeout.minutes", "15");
        properties.setProperty("license.key", "");
        properties.setProperty("installation.id", generateInstallationId());
    }
    
    private static String generateInstallationId() {
        // Generate unique ID based on MAC address + timestamp
        return System.currentTimeMillis() + "-" + 
               System.getenv("COMPUTERNAME") + "-" +
               System.getenv("USERNAME");
    }
    
    private static void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "CTH Tools Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }
    
    public static int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static double getDoubleProperty(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}