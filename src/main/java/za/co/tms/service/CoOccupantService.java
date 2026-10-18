package za.co.tms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.*;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.repository.CoOccupantRepository;
import za.co.tms.repository.TenantRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoOccupantService {

    private final CoOccupantRepository coOccupantRepository;
    private final TenantRepository tenantRepository;
    private final AppUserService appUserService;
    private final AppUserRepository appUserRepository;
    private final TenantService tenantService;

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

    /**
     * One-time maintenance: reconcile orphaned co-occupants. Finds every co-occupant record and
     * co-occupant login account that is still active but whose main tenant is already INACTIVE
     * (e.g. tenants deactivated before the offboarding cascade existed) and soft-closes them.
     *
     * <p>Idempotent — running it again after a clean sweep closes nothing. Returns a small summary
     * of how many records and login accounts were closed.
     *
     * @return a map with {@code coOccupantsClosed} and {@code loginAccountsClosed} counts
     */
    @Transactional
    public Map<String, Integer> reconcileOrphanedCoOccupants() {
        // 1) Close co-occupant records whose main tenant is INACTIVE.
        List<CoOccupant> orphanRecords =
                coOccupantRepository.findActiveWithTenantStatus(Status.CLOSED, TenantStatus.INACTIVE);
        for (CoOccupant coOccupant : orphanRecords) {
            coOccupant.setStatus(Status.CLOSED);
        }
        if (!orphanRecords.isEmpty()) {
            coOccupantRepository.saveAll(orphanRecords);
        }

        // 2) Close co-occupant login accounts whose linked tenant is INACTIVE.
        List<AppUser> orphanAccounts = appUserRepository.findActiveByRoleWithTenantStatus(
                UserRoles.CO_OCCUPANT, Status.CLOSED, TenantStatus.INACTIVE);
        for (AppUser user : orphanAccounts) {
            user.setStatus(Status.CLOSED);
            user.setDateModified(LocalDateTime.now());
        }
        if (!orphanAccounts.isEmpty()) {
            appUserRepository.saveAll(orphanAccounts);
        }

        log.info("Reconcile orphaned co-occupants: closed {} record(s) and {} login account(s)",
                orphanRecords.size(), orphanAccounts.size());

        return Map.of(
                "coOccupantsClosed", orphanRecords.size(),
                "loginAccountsClosed", orphanAccounts.size());
    }

    /**
     * Promotes a co-occupant (dependent) into a full primary tenant — used when a co-occupant
     * takes over the lease, typically because the current main tenant is moving out.
     *
     * <p>Steps:
     * <ol>
     *   <li>Create a new {@link Tenant} from the co-occupant's details, assigned to the given room
     *       with the supplied lease terms (room occupancy + lease validation run through
     *       {@link TenantService#addTenant}).</li>
     *   <li>Re-point the co-occupant's login account (matched by email) from the old main tenant to
     *       the new tenant and upgrade its role from CO_OCCUPANT to TENANT, so they keep access as
     *       the primary tenant.</li>
     *   <li>Close the co-occupant record — they are now a tenant, not a dependent.</li>
     * </ol>
     *
     * <p>This deliberately does NOT deactivate the previous main tenant. Offboarding the outgoing
     * tenant stays a separate, explicit admin action so promotion and move-out can't be conflated
     * (and so the promotion isn't cascade-closed along with the old tenant).
     *
     * @param coOccupantId the co-occupant to promote
     * @param roomId       the room the new tenant will occupy (usually the room being vacated)
     * @param leaseStart   new lease start date (nullable)
     * @param leaseEnd     new lease end date (nullable)
     * @param paymentDay   the new tenant's rent payment day (required)
     * @param title        optional title; defaults to null
     * @return the newly created primary {@link Tenant}
     */
    @Transactional
    public Tenant promoteToPrimaryTenant(Long coOccupantId, Long roomId,
                                         java.time.LocalDate leaseStart, java.time.LocalDate leaseEnd,
                                         PaymentDay paymentDay, Title title) {
        CoOccupant coOccupant = getById(coOccupantId);

        if (coOccupant.getStatus() == Status.CLOSED) {
            throw new IllegalArgumentException("Cannot promote a co-occupant that is already inactive.");
        }
        if (paymentDay == null) {
            throw new IllegalArgumentException("A payment day is required to promote a co-occupant to a tenant.");
        }
        if (roomId == null) {
            throw new IllegalArgumentException("A room is required to promote a co-occupant to a tenant.");
        }

        // 1) Build the new primary tenant from the co-occupant's details.
        Tenant newTenant = new Tenant();
        newTenant.setName(coOccupant.getName());
        newTenant.setSurname(coOccupant.getSurname());
        newTenant.setTitle(title);
        newTenant.setEmail(coOccupant.getEmail());
        newTenant.setCellPhoneNumber(coOccupant.getCellPhoneNumber());
        newTenant.setLeaseStartDate(leaseStart);
        newTenant.setLeaseEndDate(leaseEnd);
        newTenant.setPaymentDay(paymentDay);
        newTenant.setTenantBehaviour(TenantBehaviour.GOOD);
        newTenant.setTenantStatus(TenantStatus.ACTIVE);
        newTenant.setNumberOfTenantsInUnit(1);

        Room room = new Room();
        room.setId(roomId);
        newTenant.setRoom(room);

        // addTenant resolves + occupies the room and validates the lease dates.
        Tenant savedTenant = tenantService.addTenant(newTenant);
        log.info("Promoted co-occupant {} {} to primary tenant ID {}",
                coOccupant.getName(), coOccupant.getSurname(), savedTenant.getId());

        // 2) Re-link the co-occupant's login account (matched by email) to the new tenant and
        //    upgrade its role to TENANT so they keep access as the primary tenant.
        if (coOccupant.getEmail() != null && !coOccupant.getEmail().isBlank()) {
            appUserRepository.findByEmail(coOccupant.getEmail()).ifPresent(account -> {
                appUserService.linkTenant(account.getId(), savedTenant.getId());
                appUserService.updateRole(account.getId(), UserRoles.TENANT);
                log.info("Re-linked login account {} to new tenant {} as TENANT",
                        account.getUsername(), savedTenant.getId());
            });
        }

        // 3) Close the co-occupant record — they are now a full tenant.
        coOccupant.setStatus(Status.CLOSED);
        coOccupantRepository.save(coOccupant);

        return savedTenant;
    }
}
