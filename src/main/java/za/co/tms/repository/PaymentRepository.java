package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.model.Payment;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByTenantIdAndPaymentDateBetween(Long tenantId, LocalDateTime start, LocalDateTime end);
}