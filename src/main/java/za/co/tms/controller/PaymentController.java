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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.*;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;
import za.co.tms.service.AppUserService;
import za.co.tms.service.PaymentService;
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
    private final PaymentService paymentService;
    private final AppUserService appUserService;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository, TenantRepository tenantRepository, PaymentService paymentService, AppUserService appUserService) {
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
        this.paymentService = paymentService;
        this.appUserService = appUserService;
    }

    @GetMapping("/all")
    @Operation(summary = "Get all payments", description = "Returns all payment records")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/my-payments")
    @Operation(summary = "Get my payments", description = "Returns payments for the currently authenticated tenant user")
    public ResponseEntity<List<Payment>> getMyPayments() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.findByUsername(username);

        if (user.getTenant() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Payment> payments = paymentRepository.findByTenantId(user.getTenant().getId().longValue());
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/record")
    @Operation(summary = "Record a payment", description = "Records a new payment (supports backdated entries for historical data)")
    public ResponseEntity<Payment> recordPayment(@RequestBody Payment payment) {
        return ResponseEntity.ok(paymentService.recordPayment(payment));
    }

    @DeleteMapping("/{paymentId}")
    @Operation(summary = "Delete a payment", description = "Removes a payment record")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all payments for a tenant", description = "Returns all payment records for the specified tenant ID")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<Payment>> getPaymentsByTenant(
            @Parameter(description = "Tenant ID", required = true)
            @PathVariable Long tenantId,
            @Parameter(description = "Start date for filtering")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @Parameter(description = "End date for filtering")
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
