package za.co.tms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentStatus;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private PaymentService paymentService;

    private Tenant tenant(int id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Anitta");
        tenant.setSurname("Tenant");
        tenant.setEmail("anitta@testemail.com");
        tenant.setCellPhoneNumber("0821234567");
        return tenant;
    }

    @Test
    void recordPayment_whenPaid_sendsThankYouEmailAndSms() {
        Tenant tenant = tenant(10);

        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setPaymentStatus(PaymentStatus.PAID);

        when(tenantRepository.findById(10)).thenReturn(Optional.of(tenant));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.recordPayment(payment);

        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        // Both the thank-you email and SMS must fire for a PAID payment.
        verify(emailService).sendPaymentReceivedNotification(tenant);
        verify(smsService).sendPaymentReceivedSms(tenant);
    }

    @Test
    void recordPayment_whenPending_doesNotSendAnyNotification() {
        Tenant tenant = tenant(11);

        Payment payment = new Payment();
        payment.setTenant(tenant);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(tenantRepository.findById(11)).thenReturn(Optional.of(tenant));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.recordPayment(payment);

        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
        // A PENDING payment must NOT trigger a confirmation — it only fires once marked PAID.
        verify(emailService, never()).sendPaymentReceivedNotification(any());
        verify(smsService, never()).sendPaymentReceivedSms(any());
    }

    @Test
    void recordPayment_whenPaidButNoTenant_doesNotSendNotification() {
        // A PAID payment with no resolved tenant must be saved but never attempt a send
        // (guards against NPEs when historical/orphan payments are captured).
        Payment payment = new Payment();
        payment.setTenant(null);
        payment.setPaymentStatus(PaymentStatus.PAID);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.recordPayment(payment);

        verify(emailService, never()).sendPaymentReceivedNotification(any());
        verify(smsService, never()).sendPaymentReceivedSms(any());
    }
}
