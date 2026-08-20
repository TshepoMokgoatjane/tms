package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.Status;
import za.co.tms.domain.UserRoles;
import za.co.tms.dto.DashboardDTO;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.service.DashboardService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AppUserRepository appUserRepository;

    @Autowired
    public DashboardController(DashboardService dashboardService, AppUserRepository appUserRepository) {
        this.dashboardService = dashboardService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    @GetMapping("/engagement")
    public ResponseEntity<Map<String, Object>> getEngagementStats() {
        List<UserRoles> tenantRoles = List.of(UserRoles.TENANT, UserRoles.CO_OCCUPANT);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();

        // Get all tenant/co-occupant users
        List<AppUser> allUsers = appUserRepository.findAll();
        List<AppUser> tenantUsers = allUsers.stream()
                .filter(u -> tenantRoles.contains(u.getRole()))
                .filter(u -> u.getStatus() == Status.OPEN)
                .toList();

        long totalTenantUsers = tenantUsers.size();

        // Categorize users by login activity
        List<String> activeTodayNames = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(startOfToday))
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .toList();

        List<String> activeThisWeekNames = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(startOfWeek) && !u.getLastLoginAt().isAfter(startOfToday))
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .toList();

        List<String> activeThisMonthNames = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(startOfMonth) && !u.getLastLoginAt().isAfter(startOfWeek))
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .toList();

        List<String> neverLoggedInNames = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() == null)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .toList();

        long activeToday = activeTodayNames.size();
        long activeThisWeek = activeToday + activeThisWeekNames.size();
        long activeThisMonth = activeThisWeek + activeThisMonthNames.size();
        long neverLoggedIn = neverLoggedInNames.size();

        // Recent logins for reference
        List<Map<String, Object>> recentLogins = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() != null)
                .sorted((a, b) -> b.getLastLoginAt().compareTo(a.getLastLoginAt()))
                .limit(10)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", u.getFirstName() + " " + u.getLastName());
                    item.put("username", u.getUsername());
                    item.put("role", u.getRole().name());
                    item.put("lastLoginAt", u.getLastLoginAt());
                    return item;
                })
                .toList();

        // Get users who never logged in (with full details)
        List<Map<String, Object>> neverLoggedInUsers = tenantUsers.stream()
                .filter(u -> u.getLastLoginAt() == null)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", u.getFirstName() + " " + u.getLastName());
                    item.put("username", u.getUsername());
                    item.put("role", u.getRole().name());
                    return item;
                })
                .toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTenantUsers", totalTenantUsers);
        stats.put("activeToday", activeToday);
        stats.put("activeThisWeek", activeThisWeek);
        stats.put("activeThisMonth", activeThisMonth);
        stats.put("neverLoggedIn", neverLoggedIn);
        stats.put("recentLogins", recentLogins);
        stats.put("neverLoggedInUsers", neverLoggedInUsers);
        stats.put("activeTodayNames", activeTodayNames);
        stats.put("activeThisWeekNames", activeThisWeekNames);
        stats.put("activeThisMonthNames", activeThisMonthNames);
        stats.put("neverLoggedInNames", neverLoggedInNames);

        // Website interest — all user registrations (regardless of current role)
        // This tracks how many people signed up via the website over time
        List<UserRoles> nonAdminRoles = List.of(UserRoles.USER, UserRoles.TENANT, UserRoles.CO_OCCUPANT);
        long totalRegistrations = allUsers.stream()
                .filter(u -> nonAdminRoles.contains(u.getRole()))
                .filter(u -> u.getStatus() == Status.OPEN)
                .count();
        long registeredThisWeek = allUsers.stream()
                .filter(u -> nonAdminRoles.contains(u.getRole()))
                .filter(u -> u.getDateCreated() != null && u.getDateCreated().isAfter(startOfWeek))
                .count();
        long registeredThisMonth = allUsers.stream()
                .filter(u -> nonAdminRoles.contains(u.getRole()))
                .filter(u -> u.getDateCreated() != null && u.getDateCreated().isAfter(startOfMonth))
                .count();

        stats.put("totalBrowsingUsers", totalRegistrations);
        stats.put("registeredThisWeek", registeredThisWeek);
        stats.put("registeredThisMonth", registeredThisMonth);

        return ResponseEntity.ok(stats);
    }
}
