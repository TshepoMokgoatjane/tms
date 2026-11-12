package za.co.tms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Property;
import za.co.tms.repository.PropertyRepository;
import za.co.tms.repository.RoomRepository;

/**
 * Idempotent seeder for the initial set of properties and their unit links.
 * <p>
 * For each property it reuses an existing row (matched by street address) or creates one.
 * It NEVER creates a room. Rooms are linked to their owning property ONLY by looking up an
 * already-existing room by its real code and, if that room has no property yet, setting the link.
 * An already-set {@code property_id} is never overwritten, and a code that does not exist is
 * skipped (never created).
 * <p>
 * Net effect: against current production (properties present, rooms already linked) this is a
 * no-op; against a fresh database it creates only the four properties and links whatever matching
 * rooms happen to exist. Safe to run any number of times with no duplicate properties and no
 * duplicate rooms.
 */
@Component
public class PropertySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PropertySeeder.class);

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;

    public PropertySeeder(PropertyRepository propertyRepository, RoomRepository roomRepository) {
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Property caledon = getOrCreateProperty("57 Caledon Drive", "1866");
        linkExistingRooms(caledon,
                "A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11",
                "B1", "B2", "C1");

        Property emberen = getOrCreateProperty("31 Emberen Street", "295");
        linkExistingRooms(emberen, "ERF 295 - Cottage", "ERF 295 - Main House");

        Property kamele = getOrCreateProperty("7 Kamele Crescent", "490");
        linkExistingRooms(kamele, "ERF 490");

        Property ngwe = getOrCreateProperty("12 Ngwe Street", "557");
        linkExistingRooms(ngwe, "ERF 557");
    }

    private Property getOrCreateProperty(String streetAddress, String erfNumber) {
        return propertyRepository.findByStreetAddress(streetAddress)
                .orElseGet(() -> {
                    Property property = new Property();
                    property.setStreetAddress(streetAddress);
                    property.setErfNumber(erfNumber);
                    return propertyRepository.save(property);
                });
    }

    /**
     * Links already-existing rooms to the property. A room is linked only when it exists and its
     * {@code property} is currently null; an existing link is never overwritten. Codes with no
     * matching room are skipped and never created — this is what prevents the historical
     * duplicate-room bug from ever recurring.
     */
    private void linkExistingRooms(Property property, String... codes) {
        for (String code : codes) {
            roomRepository.findByCode(code).ifPresentOrElse(room -> {
                if (room.getProperty() == null) {
                    room.setProperty(property);
                    roomRepository.save(room);
                }
            }, () -> log.debug("PropertySeeder: no room with code '{}' — skipping (not creating)", code));
        }
    }
}
