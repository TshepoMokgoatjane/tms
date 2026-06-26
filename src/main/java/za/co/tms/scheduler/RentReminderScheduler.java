package za.co.tms.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentMethod;
import za.co.tms.domain.PaymentStatus;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class RentReminderScheduler {

    private final TenantService tenantService;

    private final EmailService emailService;

    private final SmsService smsService;

    private final PaymentRepository paymentRepository;

    // Runs every day at 8 AM
    /*
    Cron Expression Explained
    0 seconds
    0 minutes
    8 hours -> 8:00 AM
    Every day
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkRentDueToday() {
        List<Tenant> allTenants = tenantService.findAllTenants();
        List<Tenant> dueToday = tenantService.getTenantsWithRentDueToday(allTenants);

        if (dueToday.isEmpty()) {
            log.info("No tenants have rent due today ({})", LocalDate.now());
            return;
        }

        log.info("Tenants with rent due today ({}):", LocalDate.now());
        dueToday.forEach(tenant ->
                log.info("Tenant: {} {}, Room: {}, Rent: {}, Payment Day: {}",
                        tenant.getTitle() + " " + tenant.getName(),
                        tenant.getSurname(),
                        tenant.getRoom() != null ? tenant.getRoom().getRoomNumber() : "N/A",
                        tenant.getRentalAmount(),
                        tenant.getPaymentDay().getLabel()
                )
        );

        log.info("Sending rent reminders to {} tenants...", dueToday.size());
        dueToday.forEach(this::processRentReminder);
    }

    private void processRentReminder(Tenant tenant) {
        try {
            // Prevent duplicate payment
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            if (paymentRepository.existsByTenantIdAndPaymentDateBetween(tenant.getId().longValue(), startOfDay, endOfDay)) {
                log.info("Payment already exists for tenant: {} {} on {}", tenant.getName(), tenant.getSurname(), LocalDate.now());
                return;
            }

            // Send reminders
            emailService.sendRentReminder(tenant);
            smsService.sendRentReminderSms(tenant);
            log.info("Reminder sent to: {} {}", tenant.getName(), tenant.getSurname());

            // Create a PENDING payment record
            Payment payment = new Payment();
            payment.setTenant(tenant);
            payment.setAmount(tenant.getRentalAmount() != null ? tenant.getRentalAmount() : BigDecimal.ZERO);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentMethod(PaymentMethod.EFT);
            payment.setPaymentStatus(PaymentStatus.PENDING);

            paymentRepository.save(payment);
            log.info("Payment record created for: {} {}", tenant.getName(), tenant.getSurname());
        } catch (Exception e) {
            log.error("Failed to process rent reminder for tenant: {} {}. Error: {}",
                    tenant.getName(), tenant.getSurname(), e.getMessage(), e);
        }
    }
}
