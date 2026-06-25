package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.NextOfKin;

import java.util.Optional;

public interface NextOfKinRepository extends JpaRepository<NextOfKin, Long> {
    Optional<NextOfKin> findByTenantId(Integer tenantId);
}
