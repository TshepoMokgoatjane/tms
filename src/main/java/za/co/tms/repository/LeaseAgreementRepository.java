package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.LeaseAgreement;

import java.util.Optional;

public interface LeaseAgreementRepository extends JpaRepository<LeaseAgreement, Long> {

    Optional<LeaseAgreement> findByTenantId(Integer tenantId);

    boolean existsByTenantId(Integer tenantId);

    void deleteByTenantId(Integer tenantId);
}
