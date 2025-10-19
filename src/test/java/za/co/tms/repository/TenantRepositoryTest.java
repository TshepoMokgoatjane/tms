package za.co.tms.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@EntityScan(basePackages = "za.co.tms.model")
@ActiveProfiles("test")
public class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Should find tenant by name ignoring case")
    void shouldFindByNameIgnoreCase() {
        // Given
        Tenant tenant = new Tenant();
        tenant.setName("Tshepo");
        tenant.setSurname("Mokgoatjane");
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now().minusDays(1));
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenantRepository.save(tenant);

        // When
        Optional<Tenant> found = tenantRepository.findByNameIgnoreCase("tshepo");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Tshepo", found.get().getName());
    }

    @Test
    @DisplayName("Should return empty when tenant name not found")
    void shouldReturnEmptyWhenNameNotFound() {
        Optional<Tenant> found = tenantRepository.findByNameIgnoreCase("nonexistent");
        assertTrue(found.isEmpty());
    }
}