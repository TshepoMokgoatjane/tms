package za.co.tms.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import za.co.tms.model.Payment;
import za.co.tms.model.PaymentStatus;
import za.co.tms.repository.PaymentRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PaymentControllerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentController paymentController;

    public PaymentControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMarkPaymentAsPaid_Success() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        ResponseEntity<Payment> response = paymentController.markPaymentAsPaid(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(PaymentStatus.PAID, response.getBody().getPaymentStatus());
    }

    @Test
    public void testMarkPaymentAsPaid_NotFound() {
        when(paymentRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<Payment> response = paymentController.markPaymentAsPaid(2L);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    public void testMarkPaymentAsFailed_Success() {
        Payment payment = new Payment();
        payment.setId(3L);
        payment.setPaymentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        ResponseEntity<Payment> response = paymentController.markPaymentAsFailed(3L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(PaymentStatus.FAILED, response.getBody().getPaymentStatus());
    }

    @Test
    public void testMarkPaymentAsFailed_NotFound() {
        when(paymentRepository.findById(4L)).thenReturn(Optional.empty());

        ResponseEntity<Payment> response = paymentController.markPaymentAsFailed(4L);

        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());
    }
}
