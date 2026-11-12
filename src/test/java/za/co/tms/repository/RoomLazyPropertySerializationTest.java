package za.co.tms.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the HTTP 500 that occurred when {@code /rooms} and {@code /tenants} endpoints
 * serialized a raw {@link Room} entity whose LAZY {@code property} association was an uninitialized
 * Hibernate ByteBuddy proxy. Jackson could not serialize the proxy's {@code hibernateLazyInitializer}
 * and failed with {@code InvalidDefinitionException}.
 *
 * <p>The persistence context is cleared after flush so that reloading the {@link Room} yields a genuine
 * ByteBuddy proxy for {@code room.property}. The proxy must NOT be touched before serialization, otherwise
 * it would be initialized and the bug would not reproduce.
 */
@DataJpaTest
@EntityScan(basePackages = "za.co.tms.domain")
@EnableJpaRepositories(basePackages = "za.co.tms.repository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class RoomLazyPropertySerializationTest {

    private static final String STREET_ADDRESS = "57 Caledon Drive";

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private TestEntityManager em;

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Serializing a Room with an uninitialized LAZY Property proxy must succeed (no ByteBuddy 500)")
    void serializesRoomWithUninitializedLazyPropertyProxy() throws Exception {
        Property property = new Property();
        property.setStreetAddress(STREET_ADDRESS);
        property.setErfNumber("1866");
        property.setLeaseFilename("Lease_57_Caledon.pdf");
        property.setLeaseContentType("application/pdf");
        property.setLeaseFileSize(1024L);
        property.setLeasePdfData(new byte[]{1, 2, 3, 4});
        property = propertyRepository.save(property);

        Room room = new Room();
        room.setCode("A1");
        room.setDescription("Unit A1");
        room.setRentalAmount(BigDecimal.valueOf(5000));
        room.setOccupied(false);
        room.setProperty(property);
        room = roomRepository.save(room);

        em.flush();
        em.clear(); // force the LAZY property to be an uninitialized ByteBuddy proxy on reload

        Room reloaded = roomRepository.findById(room.getId()).orElseThrow();
        // IMPORTANT: do NOT call reloaded.getProperty().getStreetAddress() before serializing,
        // otherwise the proxy initializes and the bug would not reproduce.

        String json = mapper.writeValueAsString(reloaded);

        assertThat(json).contains(STREET_ADDRESS);
        assertThat(json).doesNotContain("hibernateLazyInitializer");
        assertThat(json).doesNotContain("leasePdfData");
    }
}
