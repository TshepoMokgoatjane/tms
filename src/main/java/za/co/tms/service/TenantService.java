package za.co.tms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantBehaviour;
import za.co.tms.repository.TenantRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Service
public class TenantService {
	
    private final TenantRepository tenantRepository;
    
    private static List<Tenant> tenants = new ArrayList<>();
    
	private static Integer tenantsCount = 0;

    @Autowired
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
	
	static {	
		
		tenants.add(new Tenant(++tenantsCount, "Eugene","Ranga", "Mr.", 
				"eugene@gmail.com", "0115489223", "0125495569", "A1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"98563214785", true, 5500.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
		tenants.add(new Tenant(++tenantsCount, "James","Crowthorn", "Mr.", 
				"james.c@google.com", "0115489223", "0125495569", "B1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"12568368717", false, 5000.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.BAD));
		tenants.add(new Tenant(++tenantsCount, "Timothy","Winfrey", "Mr.", 
				"tim@ibm.com", "0125478963", "0125495569", "C1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"56489231575", true, 5500.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
		tenants.add(new Tenant(++tenantsCount, "Greg","Johnson", "Mr.", 
				"greg.johnson@yahoo.com", "0125478963", "0125495569", "B2",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"03254199244", true, 6500.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
		tenants.add(new Tenant(++tenantsCount, "Steve","Kelly", "Mr.", 
				"stevek@atlassian.com", "0125478963", "0125495569", "A2",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"58965218962", true, 7000.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
		tenants.add(new Tenant(++tenantsCount, "Becky","Goldberg", "Mrs.", 
				"becky.goldberg@discovery.com", "0125478963", "0125495569", "A10",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"36985214781", true, 7500.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
	}
		
	public List<Tenant> findAllTenants() {
        return tenants.stream().toList();
    }

	public Tenant findTenantByName(String name) {
		Predicate<? super Tenant> predicate = tenant -> tenant.getName().equalsIgnoreCase(name);
		Tenant tenant = tenants.stream().filter(predicate).findFirst().get();
		return tenant;
	}
	
	public Tenant findTenantById(int id) {
		Predicate<? super Tenant> predicate = tenant -> tenant.getId() == id;
		Tenant tenant = tenants.stream().filter(predicate).findFirst().get();
		return tenant;
	}
	
	public Tenant addTenant(Tenant tenant) {
		Tenant tenantRef = new Tenant(++tenantsCount, tenant.getName(),tenant.getSurname(),
				tenant.getTitle(), tenant.getEmail(), tenant.getCellPhoneNumber(),
				tenant.getAlternativeCellPhoneNumber(), tenant.getRoomNumber(),
				tenant.getNumberOfTenantsInUnit(),
				tenant.getLeaseStartDate(), tenant.getLeaseEndDate(),
				tenant.getPrepaidElectricityMeterNumber(), tenant.isDepositPaid(),
				tenant.getRental(), tenant.getPaymentDate(), tenant.getTenantBehaviour());
		tenants.add(tenantRef);
		return tenantRef;
	}
	
	public void deleteTenantById(int id) {
		Predicate<? super Tenant> predicate = tenant -> tenant.getId() == id;
		tenants.removeIf(predicate);
	}

	public void updateTenant(Tenant tenant) {
		deleteTenantById(tenant.getId());
		tenants.add(tenant);
	}
}