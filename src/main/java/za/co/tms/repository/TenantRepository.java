package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.model.Tenant;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {

	List<Tenant> findTenantByName(String name);
	List<Tenant> findTenantById(int id);
}

