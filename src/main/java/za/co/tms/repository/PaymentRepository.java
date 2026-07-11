package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentMethod;
import za.co.tms.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByTenantId(Long tenantId);
	List<Payment> findByTenantIdAndPaymentDateBetween(Long tenantId, LocalDateTime start, LocalDateTime end);
	boolean existsByTenantIdAndPaymentDateBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

	// Report queries
	List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
	List<Payment> findByPaymentStatus(PaymentStatus status);
	List<Payment> findByPaymentMethod(PaymentMethod method);
	List<Payment> findByPaymentDateBetweenAndPaymentStatus(LocalDateTime start, LocalDateTime end, PaymentStatus status);
	List<Payment> findByPaymentDateBetweenAndPaymentMethod(LocalDateTime start, LocalDateTime end, PaymentMethod method);
	List<Payment> findByPaymentDateBetweenAndPaymentStatusAndPaymentMethod(LocalDateTime start, LocalDateTime end, PaymentStatus status, PaymentMethod method);
	List<Payment> findByTenantIdAndPaymentStatus(Long tenantId, PaymentStatus status);

	@Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status AND p.tenant.tenantStatus = 'ACTIVE'")
	List<Payment> findPendingPaymentsForActiveTenants(@Param("status") PaymentStatus status);
}
