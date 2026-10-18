package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.CoOccupant;
import za.co.tms.domain.Room;
import za.co.tms.domain.Status;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.repository.CoOccupantRepository;
import za.co.tms.repository.RoomRepository;
import za.co.tms.repository.TenantRepository;
import za.co.tms.validator.TenantValidator;

import java.time.LocalDate;
import java.util.List;

@Service
public class TenantService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantService.class);
	
	private final TenantRepository tenantRepository;
    private final TenantValidator tenantValidator;
    private final RoomRepository roomRepository;
    private final CoOccupantRepository coOccupantRepository;
    private final AppUserRepository appUserRepository;
	
	@Autowired
	public TenantService(TenantRepository tenantRepository, TenantValidator tenantValidator,
                         RoomRepository roomRepository, CoOccupantRepository coOccupantRepository,
                         AppUserRepository appUserRepository) {
		this.tenantRepository = tenantRepository;
        this.tenantValidator = tenantValidator;
        this.roomRepository = roomRepository;
        this.coOccupantRepository = coOccupantRepository;
        this.appUserRepository = appUserRepository;
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

        // Mark the assigned room as occupied. The frontend may send a room object with a
        // null/empty id when no room was selected — treat that as "no room assigned" rather
        // than calling findById(null), which would otherwise surface as a generic 500.
        if (tenant.getRoom() != null && tenant.getRoom().getId() != null) {
            Room room = roomRepository.findById(tenant.getRoom().getId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            room.setOccupied(true);
            roomRepository.save(room);
            tenant.setRoom(room);
        } else {
            // No valid room selected — don't persist a dangling/empty room reference.
            tenant.setRoom(null);
        }
		
		return tenantRepository.save(tenant);
	}
	
	public void deleteTenantById(int id) {

        LOGGER.info("Deleting tenant with ID {}", id);

        Tenant tenant = findTenantById(id); // will throw if not found
        tenant.setTenantStatus(TenantStatus.INACTIVE);

        // Free up the room when tenant is deactivated, then DETACH it from this (now former)
        // tenant so the unit is cleanly available for the next tenant. The tenant's own history
        // (payments, tickets, lease dates) is unaffected — none of it is tied to the room.
        if (tenant.getRoom() != null) {
            Room room = tenant.getRoom();
            room.setOccupied(false);
            roomRepository.save(room);
            tenant.setRoom(null);
        }

        // A co-occupant (dependent) only exists under a main tenant. When the main tenant is
        // offboarded, close their co-occupants too so none are left active under a departed
        // parent (CoOccupant.tenant is non-nullable, so they must be retired, not detached).
        deactivateCoOccupantsOf(tenant);

        tenantRepository.save(tenant);
	}

    /**
     * Offboards every dependent of the given main tenant when that tenant is deactivated:
     * <ol>
     *   <li>Soft-closes all still-active co-occupant records (status = CLOSED), mirroring
     *       {@code CoOccupantService.deactivateCoOccupant}.</li>
     *   <li>Soft-closes the co-occupants' linked login accounts (role CO_OCCUPANT, status = CLOSED)
     *       so a departed dependent can no longer sign in.</li>
     * </ol>
     *
     * <p>Only CO_OCCUPANT accounts are touched here — the main tenant's own account is handled by
     * the separate account offboarding path. We close those accounts directly via the repository
     * (not {@code AppUserService.deactivateUser}) on purpose: a co-occupant account shares the
     * main tenant via {@code AppUser.tenant}, so routing through {@code deactivateUser} would
     * re-trigger the account→tenant sync and recurse back into tenant deactivation.
     */
    private void deactivateCoOccupantsOf(Tenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return;
        }

        // 1) Close the co-occupant records.
        List<CoOccupant> coOccupants =
                coOccupantRepository.findByTenantIdAndStatusNot(tenant.getId(), Status.CLOSED);
        if (!coOccupants.isEmpty()) {
            for (CoOccupant coOccupant : coOccupants) {
                coOccupant.setStatus(Status.CLOSED);
            }
            coOccupantRepository.saveAll(coOccupants);
            LOGGER.info("Closed {} co-occupant(s) for deactivated tenant ID {}", coOccupants.size(), tenant.getId());
        }

        // 2) Close the co-occupants' login accounts so departed dependents can't sign in.
        List<AppUser> coOccupantUsers =
                appUserRepository.findByTenantIdAndRole(tenant.getId(), UserRoles.CO_OCCUPANT);
        List<AppUser> toClose = coOccupantUsers.stream()
                .filter(u -> u.getStatus() != Status.CLOSED)
                .toList();
        if (!toClose.isEmpty()) {
            for (AppUser user : toClose) {
                user.setStatus(Status.CLOSED);
                user.setDateModified(java.time.LocalDateTime.now());
            }
            appUserRepository.saveAll(toClose);
            LOGGER.info("Closed {} co-occupant login account(s) for deactivated tenant ID {}", toClose.size(), tenant.getId());
        }
	}

	public Tenant updateTenant(Integer id, Tenant updatedTenant) {

        LOGGER.info("Updating tenant with ID {}", id);

        Tenant existingTenant = findTenantById(id); // will throw if not found

        // Handle room change: free old room, occupy new room
        Room oldRoom = existingTenant.getRoom();
        Room newRoom = updatedTenant.getRoom();

        boolean becomingInactive = updatedTenant.getTenantStatus() == TenantStatus.INACTIVE
                && existingTenant.getTenantStatus() != TenantStatus.INACTIVE;

        boolean becomingActive = updatedTenant.getTenantStatus() == TenantStatus.ACTIVE
                && existingTenant.getTenantStatus() != TenantStatus.ACTIVE;

        // If tenant is being deactivated, free their room and detach it so the unit is cleanly
        // available for the next tenant (history stays with this tenant record either way).
        if (becomingInactive) {
            if (oldRoom != null) {
                oldRoom.setOccupied(false);
                roomRepository.save(oldRoom);
                updatedTenant.setRoom(null);
            }
            // Retire dependents along with their main tenant (see deleteTenantById).
            deactivateCoOccupantsOf(existingTenant);
        } else if (becomingActive && newRoom != null) {
            // Tenant reactivated — mark their assigned room as occupied
            Room roomToOccupy = roomRepository.findById(newRoom.getId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            roomToOccupy.setOccupied(true);
            roomRepository.save(roomToOccupy);
        } else {
            // Normal room swap logic (tenant stays active, just changing rooms)
            if (oldRoom != null && (newRoom == null || !oldRoom.getId().equals(newRoom.getId()))) {
                oldRoom.setOccupied(false);
                roomRepository.save(oldRoom);
            }

            if (newRoom != null && (oldRoom == null || !newRoom.getId().equals(oldRoom.getId()))) {
                Room roomToOccupy = roomRepository.findById(newRoom.getId())
                        .orElseThrow(() -> new RuntimeException("Room not found"));
                roomToOccupy.setOccupied(true);
                roomRepository.save(roomToOccupy);
            }
        }

        // Merge only the fields that are allowed to be updated
        existingTenant.setName(updatedTenant.getName());
        existingTenant.setSurname(updatedTenant.getSurname());
        existingTenant.setTitle(updatedTenant.getTitle());
        existingTenant.setEmail(updatedTenant.getEmail());
        existingTenant.setCellPhoneNumber(updatedTenant.getCellPhoneNumber());
        existingTenant.setAlternativeCellPhoneNumber(updatedTenant.getAlternativeCellPhoneNumber());
        existingTenant.setRoom(updatedTenant.getRoom());
        existingTenant.setLeaseStartDate(updatedTenant.getLeaseStartDate());
        existingTenant.setLeaseEndDate(updatedTenant.getLeaseEndDate());
        existingTenant.setNumberOfTenantsInUnit(updatedTenant.getNumberOfTenantsInUnit());
        existingTenant.setPaymentDay(updatedTenant.getPaymentDay());
        existingTenant.setDepositPaid(updatedTenant.isDepositPaid());
        existingTenant.setTenantBehaviour(updatedTenant.getTenantBehaviour());
        existingTenant.setTenantStatus(updatedTenant.getTenantStatus());
        existingTenant.setDateOfBirth(updatedTenant.getDateOfBirth());

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