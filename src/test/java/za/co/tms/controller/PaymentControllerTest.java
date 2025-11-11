package za.co.tms.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import za.co.tms.model.Tenant;
import za.co.tms.model.Payment;
import za.co.tms.model.Room;
import za.co.tms.model.PaymentDay;
import za.co.tms.model.TenantStatus;
import za.co.tms.model.TenantBehaviour;
import za.co.tms.model.PaymentMethod;
import za.co.tms.model.PaymentStatus;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PaymentControllerTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentController paymentController;

    private static final String TENANT_NAME = "Tshepo";
    private static final String TENANT_SURNAME = "Mokgoatjane";
    private static final String TENANT_EMAIL = "dummy@email.com";
    private static final String CELLPHONE_NUMBER = "0123456789";
    private static final String ALTERNATIVE_CELLPHONE_NUMBER = "0987654321";
    private static final int NUMBER_OF_TENANTS_IN_UNIT = 1;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMarkPaymentAsPaid() {

        Tenant tenant = createTenant();
        Payment payment = createPayment(tenant);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        // Act
        ResponseEntity<Payment> response = paymentController.markPaymentAsPaid(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PaymentStatus.PAID, response.getBody().getPaymentStatus());
    }

    @Test
    public void testMarkPaymentAsPaid_NotFound() {
        when(paymentRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<Payment> response = paymentController.markPaymentAsPaid(2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testMarkPaymentAsFailed() {

        Tenant tenant = createTenant();
        Payment payment = createPayment(tenant);

        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        ResponseEntity<Payment> response = paymentController.markPaymentAsFailed(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PaymentStatus.FAILED, response.getBody().getPaymentStatus());
    }

    @Test
    public void testMarkPaymentAsFailed_NotFound() {
        when(paymentRepository.findById(4L)).thenReturn(Optional.empty());

        ResponseEntity<Payment> response = paymentController.markPaymentAsFailed(4L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    private Tenant createTenant() {

        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setCellPhoneNumber(CELLPHONE_NUMBER);
        tenant.setAlternativeCellPhoneNumber(ALTERNATIVE_CELLPHONE_NUMBER);
        tenant.setRoomNumber(Room.A11);
        tenant.setNumberOfTenantsInUnit(NUMBER_OF_TENANTS_IN_UNIT);
        tenant.setPaymentDay(PaymentDay.DAY_1);
        tenant.setEmail(TENANT_EMAIL);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(5000));
        tenant.setTenantBehaviour(TenantBehaviour.GOOD);

        return tenant;
    }

    private Payment createPayment(Tenant tenant) {

        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setAmount(BigDecimal.valueOf(5000));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(PaymentMethod.EFT);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        return payment;
    }
}
