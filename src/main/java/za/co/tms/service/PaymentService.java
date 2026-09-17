package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentDay;
import za.co.tms.domain.PaymentStatus;
import za.co.tms.domain.Tenant;
import za.co.tms.dto.RentStatusDTO;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;

    /** Grace period (in days) after the due date before rent is treated as overdue. Configurable. */
    @Value("${rent.grace-period-days:3}")
    private int gracePeriodDays;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, TenantRepository tenantRepository) {
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public List<Payment> findByTenantId(Long tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment with ID " + id + " not found"));
    }

    public Payment recordPayment(Payment payment) {
        LOGGER.info("Recording payment for tenant ID {}", payment.getTenant() != null ? payment.getTenant().getId() : "null");

        // Validate tenant exists
        if (payment.getTenant() != null && payment.getTenant().getId() != null) {
            Tenant tenant = tenantRepository.findById(payment.getTenant().getId())
                    .orElseThrow(() -> new RuntimeException("Tenant with ID " + payment.getTenant().getId() + " not found"));
            payment.setTenant(tenant);
        }

        return paymentRepository.save(payment);
    }

    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        LOGGER.info("Updating payment {} status to {}", paymentId, status);
        Payment payment = findById(paymentId);
        payment.setPaymentStatus(status);
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        LOGGER.info("Deleting payment with ID {}", id);
        Payment payment = findById(id);
        paymentRepository.delete(payment);
    }

    /**
     * Computes the rent payment status for a tenant's current billing period.
     *
     * <p>The "current period" is the most recent period whose due date has arrived:
     * if today is on/after this month's due date, the current period is this month;
     * otherwise it is the previous month (this month's rent isn't owed until its due date).
     *
     * <p>Rent is considered PAID for the period if a PAID payment exists dated on/after
     * the period's due date. The overdue banner shows only once the payment is unpaid AND
     * today is past the due date + grace period.
     */
    public RentStatusDTO computeRentStatus(Tenant tenant, LocalDate today) {
        PaymentDay paymentDay = tenant.getPaymentDay();
        BigDecimal amountDue = tenant.getRentalAmount() != null ? tenant.getRentalAmount() : BigDecimal.ZERO;

        if (paymentDay == null) {
            // No payment day configured — nothing to enforce.
            return new RentStatusDTO(true, null, 0, false, gracePeriodDays, amountDue);
        }

        // Determine the current period's due date (clamped to the month length).
        LocalDate thisMonthDueDate = paymentDay.resolveDueDate(YearMonth.from(today));
        LocalDate dueDate = today.isBefore(thisMonthDueDate)
                ? paymentDay.resolveDueDate(YearMonth.from(today).minusMonths(1))
                : thisMonthDueDate;

        // Paid if a PAID payment exists dated on/after the period's due date.
        LocalDateTime periodStart = dueDate.atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(1).atStartOfDay().minusNanos(1);
        boolean paid = paymentRepository.existsByTenantIdAndPaymentStatusAndPaymentDateBetween(
                tenant.getId().longValue(), PaymentStatus.PAID, periodStart, windowEnd);

        long daysOverdue = paid ? 0 : Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
        boolean showBanner = !paid && daysOverdue > gracePeriodDays;

        return new RentStatusDTO(paid, dueDate, daysOverdue, showBanner, gracePeriodDays, amountDue);
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }
}
