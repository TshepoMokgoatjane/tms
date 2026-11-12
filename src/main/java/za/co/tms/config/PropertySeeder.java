package za.co.tms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;
import za.co.tms.repository.PropertyRepository;
import za.co.tms.repository.RoomRepository;

/**
 * Idempotent seeder for the initial set of properties and their units.
 * <p>
 * For each property it reuses an existing row (matched by street address) or creates one.
 * For each listed unit code it reuses the existing room (matched by code) and only sets the
 * property link; if the room does not exist it is created with the given description and linked.
 * Safe to run repeatedly — re-running neither duplicates rows nor changes existing room
 * descriptions/labels.
 */
@Component
public class PropertySeeder implements CommandLineRunner {

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
        getOrCreateUnit(emberen, "EMBEREN-COTTAGE", "Cottage");
        getOrCreateUnit(emberen, "EMBEREN-MAIN", "Main House");

        Property kamele = getOrCreateProperty("7 Kamele Crescent", "490");
        getOrCreateUnit(kamele, "KAMELE-MAIN", "Main House");

        Property ngwe = getOrCreateProperty("12 Ngwe Street", "557");
        getOrCreateUnit(ngwe, "NGWE-MAIN", "Main House");
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
     * Links already-existing rooms to the property. Rooms that are not present are skipped
     * (they were pre-existing units that should already be in the room table); their
     * descriptions/labels are never changed.
     */
    private void linkExistingRooms(Property property, String... codes) {
        for (String code : codes) {
            roomRepository.findByCode(code).ifPresent(room -> {
                if (room.getProperty() == null) {
                    room.setProperty(property);
                    roomRepository.save(room);
                }
            });
        }
    }

    /**
     * Reuses an existing unit by code (only setting the property link) or creates it with the
     * given description linked to the property.
     */
    private void getOrCreateUnit(Property property, String code, String description) {
        roomRepository.findByCode(code).ifPresentOrElse(room -> {
            if (room.getProperty() == null) {
                room.setProperty(property);
                roomRepository.save(room);
            }
        }, () -> {
            Room room = new Room();
            room.setCode(code);
            room.setDescription(description);
            room.setProperty(property);
            roomRepository.save(room);
        });
    }
}
