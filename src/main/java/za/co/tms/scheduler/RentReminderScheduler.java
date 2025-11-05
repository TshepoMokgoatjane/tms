package za.co.tms.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import za.co.tms.model.Tenant;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class RentReminderScheduler {

    private final TenantService tenantService;

    private final EmailService emailService;

    private final SmsService smsService;

    public RentReminderScheduler(TenantService tenantService, EmailService emailService, SmsService smsService) {
        this.tenantService = tenantService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    // Runs every day at 8 AM
    /*
    Cron Expression Explained
    0 seconds
    0 minutes
    8 hours -> 8:00 AM
    Every day
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkRentDueToday() {
        List<Tenant> allTenants = tenantService.findAllTenants();
        List<Tenant> dueToday = tenantService.getTenantsWithRentDueToday(allTenants);

        if (dueToday.isEmpty()) {
            log.info("No tenants have rent due today ({})", LocalDate.now());
        } else {
            log.info("Tenants with rent due today ({}):", LocalDate.now());
            dueToday.forEach(tenant ->
                    log.info("Tenant: {} {}, Room: {} {}, Payment Day: {}",
                            tenant.getTitle() + " " + tenant.getName(),
                            tenant.getSurname(),
                            tenant.getRoomNumber(),
                            tenant.getRental(),
                            tenant.getPaymentDay().getLabel()
                    )
            );

            log.info("Sending rent reminders to {} tenants...", dueToday.size());
            dueToday.forEach(tenant ->  {
                emailService.sendRentReminder(tenant);
                smsService.sendRentReminderSms(tenant);
                log.info("Reminder sent to: {} {}", tenant.getName(), tenant.getSurname());
            });
        }
    }
}