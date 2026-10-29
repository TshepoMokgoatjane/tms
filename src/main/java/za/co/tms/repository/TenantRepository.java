package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

	Optional<Tenant> findByNameIgnoreCase(String name);

	// Finds a tenant occupying the given room whose status is NOT the supplied one. Callers pass
	// TenantStatus.INACTIVE so both ACTIVE and SUSPENDED tenants count as occupying the room.
	Optional<Tenant> findByRoomIdAndTenantStatusNot(Long roomId, TenantStatus tenantStatus);
}
