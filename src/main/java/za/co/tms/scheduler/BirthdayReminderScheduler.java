package za.co.tms.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import za.co.tms.domain.Tenant;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class BirthdayReminderScheduler {

    private final TenantService tenantService;
    private final EmailService emailService;
    private final SmsService smsService;

    // Runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendBirthdayWishes() {
        LocalDate today = LocalDate.now();
        log.info("Checking for tenant birthdays on {}", today);

        List<Tenant> allTenants = tenantService.findAllTenants();

        List<Tenant> birthdayTenants = allTenants.stream()
                .filter(t -> t.getDateOfBirth() != null)
                .filter(t -> t.getDateOfBirth().getMonthValue() == today.getMonthValue()
                          && t.getDateOfBirth().getDayOfMonth() == today.getDayOfMonth())
                .toList();

        if (birthdayTenants.isEmpty()) {
            log.info("No tenant birthdays today ({})", today);
            return;
        }

        log.info("Found {} tenant(s) with birthdays today, sending wishes...", birthdayTenants.size());

        for (Tenant tenant : birthdayTenants) {
            log.info("Sending birthday wishes to {} {} ({})", tenant.getName(), tenant.getSurname(), tenant.getDateOfBirth());
            try {
                emailService.sendBirthdayNotification(tenant);
                smsService.sendBirthdaySms(tenant);
            } catch (Exception e) {
                log.error("Failed to send birthday wishes to tenant {} {}: {}", tenant.getName(), tenant.getSurname(), e.getMessage());
            }
        }

        log.info("Birthday wishes sent to {} tenant(s)", birthdayTenants.size());
    }
}
