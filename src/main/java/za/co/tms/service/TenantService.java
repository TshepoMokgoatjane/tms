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
				"eugene@gmail.com", "011548922365", "012549556996", "A1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"9856321478523689644", true, 5500.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.GOOD));
		tenants.add(new Tenant(++tenantsCount, "James","Crowthorn", "Mr.", 
				"james.c@google.com", "011548922365", "012549556996", "B1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"1256836871782225566", false, 5000.00, LocalDate.now().plusYears(10), 
				TenantBehaviour.BAD));
		tenants.add(new Tenant(++tenantsCount, "Timothy","Winfrey", "Mr.", 
				"tim@ibm.com", "01254789635", "012549556996", "C1",
				5, LocalDate.now().plusYears(10), LocalDate.now().plusYears(10),
				"5648923157525055665", true, 5500.00, LocalDate.now().plusYears(10), 
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
				tenant.getPrepaidElectricityMeter(), tenant.isDepositPaid(),
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