package za.co.tms.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@EntityScan(basePackages = "za.co.tms.domain")
@EnableJpaRepositories(basePackages = "za.co.tms.repository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class PropertyRepositoryTest {

    private static final String STREET_ADDRESS = "57 Caledon Drive";
    private static final String ERF_NUMBER = "1866";

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Property testProperty;

    @BeforeEach
    void setUp() {
        testProperty = new Property();
        testProperty.setStreetAddress(STREET_ADDRESS);
        testProperty.setErfNumber(ERF_NUMBER);
        testProperty.setLeaseFilename("Lease_57_Caledon.pdf");
        testProperty.setLeaseContentType("application/pdf");
        testProperty.setLeaseFileSize(1024L);
        testProperty.setLeasePdfData(new byte[]{1, 2, 3, 4});
        testProperty = propertyRepository.save(testProperty);
    }

    @Test
    @DisplayName("Should save and find property by street address")
    void shouldFindByStreetAddress() {
        Optional<Property> found = propertyRepository.findByStreetAddress(STREET_ADDRESS);

        assertTrue(found.isPresent());
        assertEquals(ERF_NUMBER, found.get().getErfNumber());
    }

    @Test
    @DisplayName("Should report existence by street address")
    void shouldReportExistsByStreetAddress() {
        assertTrue(propertyRepository.existsByStreetAddress(STREET_ADDRESS));
        assertFalse(propertyRepository.existsByStreetAddress("99 Nowhere Street"));
    }

    @Test
    @DisplayName("findAllMetadata should never select the lease blob")
    void findAllMetadataShouldNotSelectBlob() {
        List<Property> metadata = propertyRepository.findAllMetadata();

        assertFalse(metadata.isEmpty());
        Property property = metadata.stream()
                .filter(p -> STREET_ADDRESS.equals(p.getStreetAddress()))
                .findFirst()
                .orElseThrow();
        assertNull(property.getLeasePdfData());
        assertEquals("Lease_57_Caledon.pdf", property.getLeaseFilename());
    }

    @Test
    @DisplayName("Room saved with a property should reload with the relationship resolved")
    void roomShouldReloadWithPropertyRelationship() {
        Room room = new Room();
        room.setCode("A1");
        room.setDescription("Unit A1");
        room.setRentalAmount(BigDecimal.valueOf(5000));
        room.setOccupied(false);
        room.setProperty(testProperty);
        Room saved = roomRepository.save(room);

        Room reloaded = roomRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(reloaded.getProperty());
        assertEquals(STREET_ADDRESS, reloaded.getProperty().getStreetAddress());
    }
}
