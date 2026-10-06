package com.cth.utils;

import java.io.File;

public class PathUtils {
    // Use project folder instead of user home
    private static final String APP_DIR = System.getProperty("user.dir") + File.separator + "CTH_Tools_System";
    private static final String BACKUP_DIR = "backups";
    private static final String INVOICE_DIR = "invoices";
    private static final String LOG_DIR = "logs";
    
    public static void createDirectories() {
        String appPath = APP_DIR;
        
        createDirectory(appPath);
        createDirectory(appPath + File.separator + BACKUP_DIR);
        createDirectory(appPath + File.separator + INVOICE_DIR);
        createDirectory(appPath + File.separator + LOG_DIR);
    }
    
    private static void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("📁 Created directory: " + path);
        }
    }
    
    public static String getAppPath() {
        return APP_DIR;
    }
    
    public static String getBackupPath() {
        return getAppPath() + File.separator + BACKUP_DIR;
    }
    
    public static String getInvoicePath() {
        return getAppPath() + File.separator + INVOICE_DIR;
    }
    
    public static String getLogPath() {
        return getAppPath() + File.separator + LOG_DIR;
    }
}