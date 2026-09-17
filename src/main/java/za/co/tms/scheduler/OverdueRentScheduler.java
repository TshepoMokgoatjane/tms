package za.co.tms.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;
import za.co.tms.dto.RentStatusDTO;
import za.co.tms.service.EmailService;
import za.co.tms.service.PaymentService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends escalating overdue-rent notices (email + SMS) to active tenants whose rent
 * is past the grace period and still unpaid for the current billing period.
 *
 * <p>Runs daily. It relies on {@link PaymentService#computeRentStatus} for the
 * "unpaid and past grace" decision, so a tenant stops receiving these notices as
 * soon as their payment is marked PAID for the period.
 */
@Slf4j
@Component
@AllArgsConstructor
public class OverdueRentScheduler {

    private final TenantService tenantService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final SmsService smsService;

    // Runs every day at 08:30 (just after the rent-due reminder at 08:00)
    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void notifyOverdueTenants() {
        LocalDate today = LocalDate.now();
        List<Tenant> allTenants = tenantService.findAllTenants();

        int notified = 0;
        for (Tenant tenant : allTenants) {
            // Only chase active tenants who are still in the property.
            if (tenant.getTenantStatus() != TenantStatus.ACTIVE) {
                continue;
            }

            RentStatusDTO status = paymentService.computeRentStatus(tenant, today);

            // showBanner is true only when unpaid AND past the grace period.
            if (!status.isShowBanner()) {
                continue;
            }

            try {
                emailService.sendOverdueRentNotice(tenant, status.getDaysOverdue());
                smsService.sendOverdueRentSms(tenant, status.getDaysOverdue());
                notified++;
            } catch (Exception e) {
                log.error("Failed to send overdue rent notice to tenant {} {}: {}",
                        tenant.getName(), tenant.getSurname(), e.getMessage(), e);
            }
        }

        log.info("Overdue rent notices sent to {} tenant(s) on {}", notified, today);
    }
}
