package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.model.Payment;
import za.co.tms.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Controller", description = "Endpoints for managing tenant payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Operation(summary = "Get all payments for a tenant", description = "Returns all payment records for the specified tenant ID")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Optional<Payment>> getPaymentsByTenant(
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

        Optional<Payment> payments;

        if (startDate != null && endDate != null) {
            payments = paymentRepository.findByTenantIdAndPaymentDateBetween(tenantId, startDate, endDate);
        } else {
            payments = paymentRepository.findByTenantId(tenantId);
        }

        return ResponseEntity.ok(payments);
    }
}