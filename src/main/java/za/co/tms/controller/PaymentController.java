package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.model.*;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Controller", description = "Endpoints for managing tenant payments")
public class PaymentController {

    private final TenantRepository tenantRepository;

    private final PaymentRepository paymentRepository;

    private static final String TENANT_NAME = "Tshepo";
    private static final String TENANT_SURNAME = "Mokgoatjane";
    private static final String TENANT_EMAIL = "dummy@email.com";
    private static final String CELLPHONE_NUMBER = "0123456789";
    private static final String ALTERNATIVE_CELLPHONE_NUMBER = "0987654321";
    private static final int NUMBER_OF_TENANTS_IN_UNIT = 1;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository, TenantRepository tenantRepository) {
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
    }

    @Operation(summary = "Get all payments for a tenant", description = "Returns all payment records for the specified tenant ID")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<Payment>> getPaymentsByTenant(
            @Parameter(description = "Tenant ID", required = true)
            @PathVariable Long tenantId,

            @Parameter(description = "Start date for filtering", required = false)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @Parameter(description = "End date for filtering", required = false)
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        List<Payment> payments;

        if (startDate != null && endDate != null) {
            payments = paymentRepository.findByTenantIdAndPaymentDateBetween(tenantId, startDate, endDate);
        } else {
            payments = paymentRepository.findByTenantId(tenantId);
        }

        return ResponseEntity.ok(payments);
    }

    @Transactional
    @PutMapping("/{paymentId}/mark-paid")
    @Operation(summary = "Mark payment as PAID", description = "Updates the payment status to PAID for the given payment ID")
    public ResponseEntity<Payment> markPaymentAsPaid(
            @Parameter(description = "ID of the payment to mark as PAID")
            @PathVariable Long paymentId
    ) {
        log.info("Checking payment existence for ID: {}", paymentId);
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        if (optionalPayment.isEmpty()) {
            log.warn("Payment ID [{}] not found, returning HTTP Status Code 404", paymentId);
            return ResponseEntity.notFound().build();
        }

        Payment payment = optionalPayment.get();

        // Validate tenant existence
        if (payment.getTenant() == null) {
            log.error("Exception occurred! Payment [{}] has no associated tenant. Cannot mark as PAID.", paymentId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Ensure paymentDate is valid
        if (payment.getPaymentDate() == null || payment.getPaymentDate().isBefore(LocalDateTime.now())) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        // Update payment status and method
        payment.setPaymentMethod(PaymentMethod.EFT);
        payment.setPaymentStatus(PaymentStatus.PAID);

        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment [{}] marked as PAID successfully.", paymentId);

        return ResponseEntity.ok(updatedPayment);
    }

    @Transactional
    @Operation(summary = "Mark payment as FAILED", description = "Updates the payment status to FAILED for the given payment ID")
    @PutMapping("/{paymentId}/mark-failed")
    public ResponseEntity<Payment> markPaymentAsFailed(
            @Parameter(description = "ID of the payment to mark as FAILED")
            @PathVariable Long paymentId
    ) {
        log.info("Checking payment existence for payment ID: {}", paymentId);
        Optional<Payment> optionalPayment = paymentRepository.findById(paymentId);
        if (optionalPayment.isEmpty()) {
            log.warn("Payment ID [{}] not found, returning HTTP Status Code 404.", paymentId);
            return ResponseEntity.notFound().build();
        }

        Payment payment = optionalPayment.get();

        // Validate tenant existence
        if (payment.getTenant() == null) {
            log.error("Payment [{}] has no associated tenant. Cannot mark as PAID.", paymentId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Ensure paymentDate is valid
        if (payment.getPaymentDate() == null || payment.getPaymentDate().isBefore(LocalDateTime.now())) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        // Update payment status and method
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setPaymentStatus(PaymentStatus.FAILED);

        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment [{}] marked as FAILED successfully.", paymentId);

        return ResponseEntity.ok(updatedPayment);
    }
}