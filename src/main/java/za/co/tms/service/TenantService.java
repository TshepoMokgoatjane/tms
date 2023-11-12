package za.co.tms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.model.Tenant;
import za.co.tms.repository.TenantRepository;

import java.util.List;
import java.util.function.Predicate;

@Service
public class TenantService {
	
	private TenantRepository tenantRepository;
	
	private static final int TENANT_STATUS_INACTIVE = 1;
	
	@Autowired
	public TenantService(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}
			
	public List<Tenant> findAllTenants() {
		return tenantRepository.findAll();
    }

	public Tenant findTenantByName(String name) {
		Predicate<? super Tenant> predicate = tenant -> tenant.getName().equalsIgnoreCase(name);
		Tenant tenant = tenantRepository.findTenantByName(name).stream().filter(predicate).findFirst().get();
		return tenant;
	}
	
	public Tenant findTenantById(int id) {
		Predicate<? super Tenant> predicate = tenant -> tenant.getId() == id;
		Tenant tenant = tenantRepository.findTenantById(id).stream().filter(predicate).findFirst().get();
		return tenant;
	}
	
	public Tenant addTenant(Tenant tenant) {
		tenant.setId(null);
		
		return tenantRepository.save(tenant);
	}
	
	public void deleteTenantById(int id) {
		Tenant tenant = findTenantById(id);
		tenant.setTenantStatus(TENANT_STATUS_INACTIVE);
		
		tenantRepository.save(tenant);
	}

	public void updateTenant(Tenant tenant) {
		deleteTenantById(tenant.getId());
		tenantRepository.save(tenant);
	}
}