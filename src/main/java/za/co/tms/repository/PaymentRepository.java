package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.model.Payment;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTenantId(Long tenantId);
    Optional<Payment> findByTenantIdAndPaymentDateBetween(Long tenantId, LocalDateTime start, LocalDateTime end);
}