package com.cth.utils;


import com.cth.ui.*;
import com.cth.database.DatabaseConnection;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;

public class BackupScheduler {
    private static Timer timer;
    
    public static void startScheduler() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new Timer(true);
        
        // Schedule daily backup at 2:00 AM
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (initialDelay < 0) {
            initialDelay += 24 * 60 * 60 * 1000;
        }
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    AuditLogger.log("SYSTEM", "Scheduled Backup", "Performing scheduled backup");
                    DatabaseConnection.performBackup();
                } catch (Exception e) {
                    // Log error but don't crash
                    System.err.println("Scheduled backup failed: " + e.getMessage());
                }
            }
        }, initialDelay, 24 * 60 * 60 * 1000);
    }
    
    public static void stopScheduler() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}