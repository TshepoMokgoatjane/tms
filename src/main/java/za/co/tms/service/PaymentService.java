package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentStatus;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.util.List;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, TenantRepository tenantRepository) {
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public List<Payment> findByTenantId(Long tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment with ID " + id + " not found"));
    }

    public Payment recordPayment(Payment payment) {
        LOGGER.info("Recording payment for tenant ID {}", payment.getTenant() != null ? payment.getTenant().getId() : "null");

        // Validate tenant exists
        if (payment.getTenant() != null && payment.getTenant().getId() != null) {
            Tenant tenant = tenantRepository.findById(payment.getTenant().getId())
                    .orElseThrow(() -> new RuntimeException("Tenant with ID " + payment.getTenant().getId() + " not found"));
            payment.setTenant(tenant);
        }

        return paymentRepository.save(payment);
    }

    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        LOGGER.info("Updating payment {} status to {}", paymentId, status);
        Payment payment = findById(paymentId);
        payment.setPaymentStatus(status);
        return paymentRepository.save(payment);
    }

    public void deletePayment(Long id) {
        LOGGER.info("Deleting payment with ID {}", id);
        Payment payment = findById(id);
        paymentRepository.delete(payment);
    }
}
