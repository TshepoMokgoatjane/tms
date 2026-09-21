package za.co.tms.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Room;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.RoomRepository;
import za.co.tms.service.RoomService;
import za.co.tms.service.TenantService;

import java.time.LocalDate;
import java.util.List;

/**
 * Keeps the public "available apartments" advert in sync with reality, automatically.
 *
 * <p>Runs daily and does two things:
 * <ol>
 *   <li><b>Opens</b> advertising for a unit about a month before it frees up: when an occupying
 *       tenant's {@code leaseEndDate} is within {@link #ADVERTISE_LEAD_DAYS} days, the unit's room
 *       is flagged as advertised with {@code availableFrom = leaseEndDate}.</li>
 *   <li><b>Closes</b> advertising for any unit that has become occupied again, so re-let units
 *       drop off the advert without any manual step.</li>
 * </ol>
 *
 * <p>Admins can still advertise / stop advertising a unit manually at any time; this scheduler only
 * automates the common lease-ending case and the clean-up of re-occupied units.
 */
@Slf4j
@Component
@AllArgsConstructor
public class AdvertisingScheduler {

    /** Advertise a unit this many days before the current lease ends (roughly a month). */
    private static final int ADVERTISE_LEAD_DAYS = 30;

    private final TenantService tenantService;
    private final RoomService roomService;
    private final RoomRepository roomRepository;

    // Runs every day at 06:00, before the day's tenant-facing notices.
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void refreshAdvertisedUnits() {
        LocalDate today = LocalDate.now();
        LocalDate leadWindowEnd = today.plusDays(ADVERTISE_LEAD_DAYS);

        int opened = 0;
        int closed = 0;

        // 1) Open advertising for units whose lease ends within the lead window.
        List<Tenant> tenants = tenantService.findAllTenants();
        for (Tenant tenant : tenants) {
            Room room = tenant.getRoom();
            LocalDate leaseEnd = tenant.getLeaseEndDate();
            if (room == null || leaseEnd == null) {
                continue;
            }
            // Lease ends between today and the lead window (inclusive) and the unit isn't already advertised.
            boolean withinLeadWindow = !leaseEnd.isBefore(today) && !leaseEnd.isAfter(leadWindowEnd);
            if (withinLeadWindow && !room.isAdvertised()) {
                try {
                    roomService.advertiseRoom(room.getId(), null, null, leaseEnd, null);
                    opened++;
                    log.info("Auto-advertising room {} (code {}); lease ends {}",
                            room.getId(), room.getCode(), leaseEnd);
                } catch (Exception e) {
                    log.error("Failed to auto-advertise room {}: {}", room.getId(), e.getMessage(), e);
                }
            }
        }

        // 2) Close advertising for any unit that is occupied again.
        for (Room room : roomRepository.findByAdvertisedTrue()) {
            if (room.isOccupied()) {
                room.setAdvertised(false);
                room.setAdvertisingEndDate(today);
                roomRepository.save(room);
                closed++;
                log.info("Stopped advertising room {} (code {}) — now occupied", room.getId(), room.getCode());
            }
        }

        log.info("Advertising refresh complete on {}: {} opened, {} closed", today, opened, closed);
    }
}
