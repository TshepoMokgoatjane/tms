package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.Tenant;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

	Optional<Tenant> findByNameIgnoreCase(String name);
}