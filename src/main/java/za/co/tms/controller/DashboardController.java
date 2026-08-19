package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

        long totalTenantUsers = appUserRepository.countByRoleInAndStatusIs(tenantRoles, Status.OPEN);
        long activeToday = appUserRepository.countByLastLoginAtAfter(startOfToday);
        long activeThisWeek = appUserRepository.countByLastLoginAtAfter(startOfWeek);
        long activeThisMonth = appUserRepository.countByLastLoginAtAfter(startOfMonth);
        long neverLoggedIn = appUserRepository.countByLastLoginAtIsNull();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTenantUsers", totalTenantUsers);
        stats.put("activeToday", activeToday);
        stats.put("activeThisWeek", activeThisWeek);
        stats.put("activeThisMonth", activeThisMonth);
        stats.put("neverLoggedIn", neverLoggedIn);

        return ResponseEntity.ok(stats);
    }
}
