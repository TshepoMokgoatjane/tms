package za.co.tms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.model.Tenant;
import za.co.tms.repo.TenantRepo;

import java.util.List;

@Service
public class TenantService {
    private final TenantRepo tenantRepo;

    @Autowired
    public TenantService(TenantRepo tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    public Tenant addTenant(Tenant tenant) {
        return this.tenantRepo.save(tenant);
    }

    public List<Tenant> findAllTenants() {
        return this.tenantRepo.findAll();
    }

    public Tenant updateTenant(Tenant tenant) {
        return this.tenantRepo.save(tenant);
    }

    public Tenant findTenantById(Long id) {
        return this.tenantRepo.findTenantById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant by id <" + id + "> was not found!"));
    }

    public void deleteTenant(Long id) {
        this.tenantRepo.deleteTenantById(id);
    }
}
