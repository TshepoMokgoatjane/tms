package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettingsDTO {

    // Application Info
    private String appName;
    private String appVersion;
    private String springBootVersion;
    private String javaVersion;
    private String databaseType;
    private String serverPort;
    private long uptimeMillis;
    private String environment;

    // Communication Settings
    private String schedulerCron;
    private boolean schedulerEnabled;
    private String lastSchedulerRun;

    // Notification Preferences
    private String emailProvider;
    private String emailHost;
    private int emailPort;
    private boolean emailConfigured;
    private String smsProvider;
    private boolean smsConfigured;
    private String contactNotificationEmail;

    // Business Configuration
    private String galleryUploadDir;
    private String maxFileSize;
    private boolean recaptchaEnabled;
    private int totalRooms;
    private int occupiedRooms;
    private int totalTenants;
    private int totalUsers;
}
