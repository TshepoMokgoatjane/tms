package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.exception.RoomAlreadyAssignedException;
import za.co.tms.exception.RoomNotFoundException;
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
	
	@Transactional
	public Tenant addTenant(Tenant tenant) {

        LOGGER.info("Registering a new tenant");

        tenantValidator.validateLeaseDates(tenant);

        tenant.setId(null); // Ensure it's a new tenant

        // Mark the assigned room as occupied. The frontend may send a room object with a
        // null/empty id when no room was selected — treat that as "no room assigned" rather
        // than calling findById(null), which would otherwise surface as a generic 500.
        if (tenant.getRoom() != null && tenant.getRoom().getId() != null) {
            Room room = roomRepository.findById(tenant.getRoom().getId())
                    .orElseThrow(() -> new RoomNotFoundException("Room not found"));

            // Reject assignment to a room still held by any non-INACTIVE tenant (ACTIVE or
            // SUSPENDED). This is the user-facing safety net for the one-tenant-per-room rule.
            tenantRepository.findByRoomIdAndTenantStatusNot(room.getId(), TenantStatus.INACTIVE)
                    .ifPresent(existing -> {
                        throw new RoomAlreadyAssignedException("Room " + room.getRoomNumber()
                                + " is already assigned to another tenant. Free the room first before assigning a new tenant.");
                    });

            room.setOccupied(true);
            roomRepository.save(room);
            tenant.setRoom(room);
        } else {
            // No valid room selected — don't persist a dangling/empty room reference.
            tenant.setRoom(null);
        }
		
		return tenantRepository.save(tenant);
	}
	
	@Transactional
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

	@Transactional
	public Tenant updateTenant(Integer id, Tenant updatedTenant) {

        LOGGER.info("Updating tenant with ID {}", id);

        Tenant existingTenant = findTenantById(id); // will throw if not found

        // Handle room change: free old room, occupy new room
        Room oldRoom = existingTenant.getRoom();
        Room newRoom = updatedTenant.getRoom();

        // The room link this tenant ends up with. Resolved explicitly on EVERY branch so the
        // final merge never re-derives it from updatedTenant (which carries a detached/unmanaged
        // room) — reactivation must not null the link and deactivation must reliably free the room.
        Room resolvedRoom = oldRoom;

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
            }
            resolvedRoom = null;
            // Retire dependents along with their main tenant (see deleteTenantById).
            deactivateCoOccupantsOf(existingTenant);
        } else if (becomingActive && newRoom != null) {
            // Tenant reactivated — mark their assigned room as occupied, keeping the link intact.
            Room managed = roomRepository.findById(newRoom.getId())
                    .orElseThrow(() -> new RoomNotFoundException("Room not found"));
            assertRoomFreeForTenant(managed, id);
            managed.setOccupied(true);
            roomRepository.save(managed);
            resolvedRoom = managed;
        } else {
            // Normal room swap logic (tenant stays active, just changing rooms)
            if (newRoom == null) {
                if (oldRoom != null) {
                    oldRoom.setOccupied(false);
                    roomRepository.save(oldRoom);
                }
                resolvedRoom = null;
            } else if (oldRoom == null || !newRoom.getId().equals(oldRoom.getId())) {
                // Real swap to a different room: verify it is free BEFORE freeing the old one.
                Room managed = roomRepository.findById(newRoom.getId())
                        .orElseThrow(() -> new RoomNotFoundException("Room not found"));
                assertRoomFreeForTenant(managed, id);

                if (oldRoom != null) {
                    oldRoom.setOccupied(false);
                    roomRepository.save(oldRoom);
                }
                managed.setOccupied(true);
                roomRepository.save(managed);
                resolvedRoom = managed;
            }
            // Same-room update: keep oldRoom (resolvedRoom already defaults to it).
        }

        // Merge only the fields that are allowed to be updated
        existingTenant.setName(updatedTenant.getName());
        existingTenant.setSurname(updatedTenant.getSurname());
        existingTenant.setTitle(updatedTenant.getTitle());
        existingTenant.setEmail(updatedTenant.getEmail());
        existingTenant.setCellPhoneNumber(updatedTenant.getCellPhoneNumber());
        existingTenant.setAlternativeCellPhoneNumber(updatedTenant.getAlternativeCellPhoneNumber());
        existingTenant.setRoom(resolvedRoom);
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

    /**
     * Guards the one-tenant-per-room rule for updates: rejects assigning {@code room} to the
     * tenant being updated when it is still held by a DIFFERENT non-INACTIVE tenant (ACTIVE or
     * SUSPENDED). The occupant's identity is compared with {@code .equals} because Tenant.id is an
     * Integer while Room.id is a Long.
     */
    private void assertRoomFreeForTenant(Room room, Integer tenantId) {
        tenantRepository.findByRoomIdAndTenantStatusNot(room.getId(), TenantStatus.INACTIVE)
                .filter(occupant -> !occupant.getId().equals(tenantId))
                .ifPresent(occupant -> {
                    throw new RoomAlreadyAssignedException("Room " + room.getRoomNumber()
                            + " is already assigned to another tenant. Free the room first before assigning a new tenant.");
                });
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