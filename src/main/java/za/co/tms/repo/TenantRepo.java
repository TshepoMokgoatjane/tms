package za.co.tms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.model.Tenant;

import java.util.Optional;

public interface TenantRepo extends JpaRepository<Tenant, Long> {

    void deleteTenantById(Long id);
    Optional<Tenant> findTenantById(Long id);
}
