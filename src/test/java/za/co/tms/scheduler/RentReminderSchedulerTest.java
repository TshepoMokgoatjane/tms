package za.co.tms.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import za.co.tms.model.Payment;
import za.co.tms.model.PaymentDay;
import za.co.tms.model.Tenant;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RentReminderSchedulerTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RentReminderScheduler rentReminderScheduler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPaymentCreatedWhenNoneExists() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setName("John");
        tenant.setSurname("Doe");
        tenant.setRental(BigDecimal.valueOf(5000));
        tenant.setPaymentDay(PaymentDay.DAY_7); // Assuming today is the 8th

        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));
        when(tenantService.getTenantsWithRentDueToday(anyList())).thenReturn(Collections.singletonList(tenant));
        when(paymentRepository.findByTenantIdAndPaymentDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        rentReminderScheduler.checkRentDueToday();

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testPaymentNotCreatedWhenExists() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setSurname("John");
        tenant.setSurname("Doe");
        tenant.setRental(BigDecimal.valueOf(5000));
        tenant.setPaymentDay(PaymentDay.DAY_7); // Assuming today is the 8th

        Payment existingPayment = new Payment();
        existingPayment.setTenant(tenant);
        existingPayment.setPaymentDate(LocalDateTime.now());

        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));
        when(tenantService.getTenantsWithRentDueToday(anyList())).thenReturn(Collections.singletonList(tenant));
        when(paymentRepository.findByTenantIdAndPaymentDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(existingPayment));

        rentReminderScheduler.checkRentDueToday();

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
