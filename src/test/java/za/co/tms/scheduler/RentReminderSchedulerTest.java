package za.co.tms.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentDay;
import za.co.tms.domain.Room;
import za.co.tms.domain.Tenant;
import za.co.tms.dto.RentStatusDTO;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.PaymentService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TenantService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

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

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private RentReminderScheduler rentReminderScheduler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /** Helper: a "not paid for the current period" status so the reminder proceeds. */
    private RentStatusDTO notPaid() {
        return new RentStatusDTO(false, LocalDate.now(), 0, false, 3, BigDecimal.valueOf(5000));
    }

    /** Helper: a "paid for the current period" status (e.g. an early payment). */
    private RentStatusDTO paid() {
        return new RentStatusDTO(true, LocalDate.now(), 0, false, 3, BigDecimal.valueOf(5000));
    }

    @Test
    public void testPaymentCreatedWhenNoneExists() {
        Room room = new Room();
        room.setRentalAmount(BigDecimal.valueOf(5000));

        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setName("John");
        tenant.setSurname("Doe");
        tenant.setRoom(room);
        tenant.setPaymentDay(PaymentDay.DAY_7); // Assuming today is the 8th

        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));
        when(tenantService.getTenantsWithRentDueToday(anyList())).thenReturn(Collections.singletonList(tenant));
        when(paymentService.computeRentStatus(eq(tenant), any(LocalDate.class))).thenReturn(notPaid());
        when(paymentRepository.existsByTenantIdAndPaymentDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(false);

        rentReminderScheduler.checkRentDueToday();

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    public void testPaymentNotCreatedWhenExists() {
        Room room = new Room();
        room.setRentalAmount(BigDecimal.valueOf(5000));

        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setName("John");
        tenant.setSurname("Doe");
        tenant.setRoom(room);
        tenant.setPaymentDay(PaymentDay.DAY_7); // Assuming today is the 8th

        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));
        when(tenantService.getTenantsWithRentDueToday(anyList())).thenReturn(Collections.singletonList(tenant));
        when(paymentService.computeRentStatus(eq(tenant), any(LocalDate.class))).thenReturn(notPaid());
        when(paymentRepository.existsByTenantIdAndPaymentDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(true);

        rentReminderScheduler.checkRentDueToday();

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    public void earlyPayment_suppressesReminderAndDoesNotCreateDuplicate() {
        // Tenant has already paid for the current period (paid early, before the due date).
        Room room = new Room();
        room.setRentalAmount(BigDecimal.valueOf(5000));

        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setName("Jane");
        tenant.setSurname("Early");
        tenant.setRoom(room);
        tenant.setPaymentDay(PaymentDay.DAY_5);

        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));
        when(tenantService.getTenantsWithRentDueToday(anyList())).thenReturn(Collections.singletonList(tenant));
        when(paymentService.computeRentStatus(eq(tenant), any(LocalDate.class))).thenReturn(paid());

        rentReminderScheduler.checkRentDueToday();

        // No reminder sent and NO duplicate payment created for an already-paid period.
        verify(emailService, never()).sendRentReminder(any(Tenant.class));
        verify(smsService, never()).sendRentReminderSms(any(Tenant.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
