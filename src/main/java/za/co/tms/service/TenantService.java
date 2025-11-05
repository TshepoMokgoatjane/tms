package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantStatus;
import za.co.tms.repository.TenantRepository;
import za.co.tms.validator.TenantValidator;

import java.time.LocalDate;
import java.util.List;

@Service
public class TenantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantService.class);
	
	private final TenantRepository tenantRepository;

    TenantValidator tenantValidator = new TenantValidator();
	
	@Autowired
	public TenantService(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}
			
	public List<Tenant> findAllTenants() {
		return tenantRepository.findAll();
    }

	public Tenant findTenantByName(String name) {

        LOGGER.info("Find tenant with name {}", name);

        return tenantRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new TenantNotFoundException("Tenant with name '" + name + "' not found"));
	}
	
	public Tenant findTenantById(int id) {

        LOGGER.info("Find tenant with ID {}", id);

        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException("Tenant with ID " + id + " not found"));
	}
	
	public Tenant addTenant(Tenant tenant) {

        LOGGER.info("Registering a new tenant");

        tenantValidator.validateLeaseDates(tenant);

        tenant.setId(null); // Ensure it's a new tenant
		
		return tenantRepository.save(tenant);
	}
	
	public void deleteTenantById(int id) {

        LOGGER.info("Deleting tenant with ID {}", id);

        Tenant tenant = findTenantById(id); // will throw if not found
        tenant.setTenantStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
	}

	public Tenant updateTenant(Integer id, Tenant updatedTenant) {

        LOGGER.info("Updating tenant with ID {}", id);

        Tenant existingTenant = findTenantById(id); // will throw if not found

        // Merge only the fields that are allowed to be updated
        existingTenant.setName(updatedTenant.getName());
        existingTenant.setSurname(updatedTenant.getSurname());
        existingTenant.setTitle(updatedTenant.getTitle());
        existingTenant.setEmail(updatedTenant.getEmail());
        existingTenant.setCellPhoneNumber(updatedTenant.getCellPhoneNumber());
        existingTenant.setAlternativeCellPhoneNumber(updatedTenant.getAlternativeCellPhoneNumber());
        existingTenant.setRoomNumber(updatedTenant.getRoomNumber());
        existingTenant.setLeaseStartDate(updatedTenant.getLeaseStartDate());
        existingTenant.setLeaseEndDate(updatedTenant.getLeaseEndDate());
        existingTenant.setNumberOfTenantsInUnit(updatedTenant.getNumberOfTenantsInUnit());
        existingTenant.setPaymentDay(updatedTenant.getPaymentDay());
        existingTenant.setDepositPaid(updatedTenant.isDepositPaid());
        existingTenant.setRental(updatedTenant.getRental());
        existingTenant.setTenantBehaviour(updatedTenant.getTenantBehaviour());
        existingTenant.setTenantStatus(updatedTenant.getTenantStatus());

        return tenantRepository.save(existingTenant);
	}

    /*
    This method can be used to:
    Scheduled jobs to send reminders
    Dashboards to highlight due payments
    Reports to filter tenants whose rent is due today
     */
    public boolean isRentDueToday(Tenant tenant, LocalDate today) {
        return tenant.getPaymentDay().matches(today);
    }


    /*
    This method:
    Accepts a list of all tenants.
    Filters tenants whose paymentDay matches today's date using your existing isRentDueToday() method above.
    Returns a list of tenants with rent due today.
     */
    public List<Tenant> getTenantsWithRentDueToday(List<Tenant> allTenants) {
        LocalDate today = LocalDate.now();
        return allTenants.stream()
                .filter(tenant -> tenant.getPaymentDay() != null && isRentDueToday(tenant, today))
                .toList();
    }
}