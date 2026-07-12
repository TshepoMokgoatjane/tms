package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.UserRoles;
import za.co.tms.dto.SettingsDTO;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.repository.RoomRepository;
import za.co.tms.repository.TenantRepository;
import za.co.tms.service.AppUserService;

import java.lang.management.ManagementFactory;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings Controller", description = "Endpoints for application settings and configuration")
public class SettingsController {

    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    @Value("${server.port:5000}")
    private String serverPort;

    @Value("${spring.mail.host:not-configured}")
    private String emailHost;

    @Value("${spring.mail.port:0}")
    private int emailPort;

    @Value("${spring.mail.username:not-configured}")
    private String emailUsername;

    @Value("${twilio.account.sid:no-sid}")
    private String twilioSid;

    @Value("${twilio.phone.number:not-configured}")
    private String twilioPhone;

    @Value("${contact.notification.email:not-set}")
    private String contactNotificationEmail;

    @Value("${gallery.upload.dir:uploads/gallery}")
    private String galleryUploadDir;

    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSize;

    @Value("${recaptcha.secret:no-recaptcha-secret}")
    private String recaptchaSecret;

    @Value("${spring.jpa.properties.hibernate.dialect:unknown}")
    private String hibernateDialect;

    @Value("${app.backend.path:c:/devSpace/tms}")
    private String backendPath;

    @Value("${app.frontend.path:c:/devSpace/tms-front-end}")
    private String frontendPath;

    @Autowired
    public SettingsController(AppUserService appUserService,
                              AppUserRepository appUserRepository,
                              RoomRepository roomRepository,
                              TenantRepository tenantRepository) {
        this.appUserService = appUserService;
        this.appUserRepository = appUserRepository;
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @Operation(summary = "Get application settings", description = "Returns system configuration and status information")
    public ResponseEntity<SettingsDTO> getSettings() {
        SettingsDTO dto = new SettingsDTO();

        // Application Info
        dto.setAppName("TLT-Properties Tenant Management System");
        dto.setAppVersion("1.0.0");
        dto.setSpringBootVersion(SpringBootVersion.getVersion());
        dto.setJavaVersion(System.getProperty("java.version"));
        dto.setDatabaseType(getDatabaseType());
        dto.setServerPort(serverPort);
        dto.setUptimeMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        dto.setEnvironment(System.getProperty("spring.profiles.active", "default"));

        // Communication Settings
        dto.setSchedulerCron("0 0 8 * * *");
        dto.setSchedulerEnabled(true);
        dto.setLastSchedulerRun("Runs daily at 08:00 AM");

        // Notification Preferences
        dto.setEmailProvider("Gmail SMTP");
        dto.setEmailHost(emailHost);
        dto.setEmailPort(emailPort);
        dto.setEmailConfigured(!emailUsername.equals("noreply@localhost") && !emailUsername.equals("not-configured"));
        dto.setSmsProvider("Twilio");
        dto.setSmsConfigured(!twilioSid.equals("no-sid"));
        dto.setContactNotificationEmail(contactNotificationEmail);

        // Business Configuration
        dto.setGalleryUploadDir(galleryUploadDir);
        dto.setMaxFileSize(maxFileSize);
        dto.setRecaptchaEnabled(!recaptchaSecret.equals("no-recaptcha-secret"));
        dto.setTotalRooms((int) roomRepository.count());
        dto.setOccupiedRooms((int) roomRepository.findAll().stream().filter(r -> r.isOccupied()).count());
        dto.setTotalTenants((int) tenantRepository.count());
        dto.setTotalUsers((int) appUserRepository.count());

        // Project Size Info
        dto.setBackendSizeMB(getDirectorySizeMB(backendPath));
        dto.setFrontendSizeMB(getDirectorySizeMB(frontendPath));
        dto.setBackendSourceSizeMB(getDirectorySizeMB(backendPath + "/src"));
        dto.setFrontendSourceSizeMB(getDirectorySizeMB(frontendPath + "/src"));

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role", description = "Change a user's role (ADMIN only)")
    public ResponseEntity<AppUser> updateUserRole(@PathVariable int id, @RequestParam UserRoles role) {
        AppUser updated = appUserService.updateRole(id, role);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    private String getDatabaseType() {
        if (hibernateDialect.contains("MySQL")) return "MySQL";
        if (hibernateDialect.contains("PostgreSQL")) return "PostgreSQL";
        if (hibernateDialect.contains("H2")) return "H2 (Test)";
        return "Unknown";
    }

    private double getDirectorySizeMB(String path) {
        try {
            java.io.File dir = new java.io.File(path);
            if (!dir.exists()) return 0;
            long size = getDirectorySize(dir);
            return Math.round(size / (1024.0 * 1024.0) * 100.0) / 100.0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long getDirectorySize(java.io.File dir) {
        long size = 0;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirectorySize(file);
                }
            }
        }
        return size;
    }
}
