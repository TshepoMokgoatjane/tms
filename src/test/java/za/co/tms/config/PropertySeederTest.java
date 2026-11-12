package za.co.tms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;
import za.co.tms.repository.PropertyRepository;
import za.co.tms.repository.RoomRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the corrected {@link PropertySeeder}: it links pre-existing rooms to their property by
 * real code, NEVER creates a room, never overwrites an already-set property link, and never
 * duplicates properties on repeated runs.
 *
 * <p>{@code @DataJpaTest} is a slice that does not start {@code CommandLineRunner} beans, so the
 * seeder is constructed manually from the autowired repositories and {@code run()} is invoked
 * directly — matching the repository-test slice style used elsewhere in this module.
 */
@DataJpaTest
@EntityScan(basePackages = "za.co.tms.domain")
@EnableJpaRepositories(basePackages = "za.co.tms.repository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class PropertySeederTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RoomRepository roomRepository;

    private PropertySeeder seeder() {
        return new PropertySeeder(propertyRepository, roomRepository);
    }

    private Room saveRoom(String code, Property property) {
        Room room = new Room();
        room.setCode(code);
        room.setDescription(code);
        room.setRentalAmount(BigDecimal.valueOf(5000));
        room.setOccupied(false);
        room.setProperty(property);
        return roomRepository.save(room);
    }

    @Test
    @DisplayName("Links pre-existing rooms to the right property and creates no new rooms")
    void linksExistingRoomsAndCreatesNoNewRooms() {
        saveRoom("ERF 557", null);
        saveRoom("ERF 295 - Cottage", null);
        saveRoom("ERF 295 - Main House", null);
        saveRoom("ERF 490", null);
        saveRoom("A1", null);

        long before = roomRepository.count();

        seeder().run();

        // No room was created.
        assertThat(roomRepository.count()).isEqualTo(before);

        // The four properties now exist.
        assertThat(propertyRepository.findByStreetAddress("57 Caledon Drive")).isPresent();
        assertThat(propertyRepository.findByStreetAddress("31 Emberen Street")).isPresent();
        assertThat(propertyRepository.findByStreetAddress("7 Kamele Crescent")).isPresent();
        assertThat(propertyRepository.findByStreetAddress("12 Ngwe Street")).isPresent();

        // Each pre-existing room is now linked to the expected property.
        assertLinkedTo("ERF 557", "12 Ngwe Street", "557");
        assertLinkedTo("ERF 295 - Cottage", "31 Emberen Street", "295");
        assertLinkedTo("ERF 295 - Main House", "31 Emberen Street", "295");
        assertLinkedTo("ERF 490", "7 Kamele Crescent", "490");
        assertLinkedTo("A1", "57 Caledon Drive", "1866");
    }

    @Test
    @DisplayName("Running twice does not duplicate properties and never relinks an already-linked room")
    void runningTwiceDoesNotDuplicatePropertiesOrRelinkRooms() {
        // A room pre-linked to a DIFFERENT property than the seeder would assign.
        Property other = new Property();
        other.setStreetAddress("99 Somewhere Else");
        other.setErfNumber("9999");
        other = propertyRepository.save(other);
        Room preLinked = saveRoom("ERF 557", other);
        Long originalPropertyId = preLinked.getProperty().getId();

        seeder().run();
        long propertiesAfterFirstRun = propertyRepository.count();

        seeder().run();

        // No duplicate properties created on the second run.
        assertThat(propertyRepository.count()).isEqualTo(propertiesAfterFirstRun);

        // The already-set link is never overwritten.
        Room reloaded = roomRepository.findByCode("ERF 557").orElseThrow();
        assertThat(reloaded.getProperty().getId()).isEqualTo(originalPropertyId);
    }

    @Test
    @DisplayName("Never creates a room for a missing code")
    void neverCreatesRoomForMissingCode() {
        long before = roomRepository.count();

        seeder().run();

        // No room created for any absent real code or any historical invented code.
        assertThat(roomRepository.count()).isEqualTo(before);
        assertThat(roomRepository.findByCode("EMBEREN-COTTAGE")).isEmpty();
        assertThat(roomRepository.findByCode("EMBEREN-MAIN")).isEmpty();
        assertThat(roomRepository.findByCode("KAMELE-MAIN")).isEmpty();
        assertThat(roomRepository.findByCode("NGWE-MAIN")).isEmpty();
    }

    private void assertLinkedTo(String code, String expectedStreet, String expectedErf) {
        Room room = roomRepository.findByCode(code).orElseThrow();
        assertThat(room.getProperty()).as("room %s must be linked", code).isNotNull();
        assertThat(room.getProperty().getStreetAddress()).isEqualTo(expectedStreet);
        assertThat(room.getProperty().getErfNumber()).isEqualTo(expectedErf);
    }
}
