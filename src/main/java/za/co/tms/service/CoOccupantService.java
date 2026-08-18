package za.co.tms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.*;
import za.co.tms.repository.CoOccupantRepository;
import za.co.tms.repository.TenantRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoOccupantService {

    private final CoOccupantRepository coOccupantRepository;
    private final TenantRepository tenantRepository;
    private final AppUserService appUserService;

    /**
     * Link an existing registered user as a co-occupant to a primary tenant.
     * Updates the user's role to CO_OCCUPANT and links them to the tenant.
     */
    @Transactional
    public CoOccupant addCoOccupant(CoOccupant coOccupant, Integer tenantId, Integer userId) {
        // Validate tenant exists
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        // Validate and fetch the existing user
        AppUser user = appUserService.findById(userId);

        if (user.getTenant() != null) {
            throw new RuntimeException("This user is already linked to a tenant. Please unlink them first.");
        }

        coOccupant.setTenant(tenant);

        // Save the co-occupant record
        CoOccupant saved = coOccupantRepository.save(coOccupant);
        log.info("Co-occupant {} {} created for tenant {} {}",
                saved.getName(), saved.getSurname(), tenant.getName(), tenant.getSurname());

        // Update the existing user's role and link to tenant
        appUserService.updateRole(userId, UserRoles.CO_OCCUPANT);
        appUserService.linkTenant(userId, tenantId);
        log.info("User {} upgraded to CO_OCCUPANT and linked to tenant ID {}", user.getUsername(), tenantId);

        return saved;
    }

    /**
     * Get all co-occupants for a specific tenant.
     */
    public List<CoOccupant> getCoOccupantsByTenant(Integer tenantId) {
        return coOccupantRepository.findByTenantId(tenantId);
    }

    /**
     * Get all active co-occupants for a specific tenant.
     */
    public List<CoOccupant> getActiveCoOccupantsByTenant(Integer tenantId) {
        return coOccupantRepository.findByTenantIdAndStatusNot(tenantId, Status.CLOSED);
    }

    /**
     * Get all co-occupants across all tenants.
     */
    public List<CoOccupant> getAllCoOccupants() {
        return coOccupantRepository.findAll();
    }

    /**
     * Get a co-occupant by ID.
     */
    public CoOccupant getById(Long id) {
        return coOccupantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Co-occupant not found with ID: " + id));
    }

    /**
     * Update co-occupant details.
     */
    @Transactional
    public CoOccupant updateCoOccupant(Long id, CoOccupant updated) {
        CoOccupant existing = getById(id);

        existing.setName(updated.getName());
        existing.setSurname(updated.getSurname());
        existing.setRelationship(updated.getRelationship());
        existing.setEmail(updated.getEmail());
        existing.setCellPhoneNumber(updated.getCellPhoneNumber());
        existing.setVehicleRegistration(updated.getVehicleRegistration());

        log.info("Co-occupant {} {} updated", existing.getName(), existing.getSurname());
        return coOccupantRepository.save(existing);
    }

    /**
     * Deactivate a co-occupant (soft delete).
     */
    @Transactional
    public void deactivateCoOccupant(Long id) {
        CoOccupant coOccupant = getById(id);
        coOccupant.setStatus(Status.CLOSED);
        coOccupantRepository.save(coOccupant);
        log.info("Co-occupant {} {} deactivated", coOccupant.getName(), coOccupant.getSurname());
    }
}
