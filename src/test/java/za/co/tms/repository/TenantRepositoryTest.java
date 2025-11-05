package za.co.tms.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import za.co.tms.model.PaymentDay;
import za.co.tms.model.Room;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantBehaviour;
import za.co.tms.model.TenantStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@EntityScan(basePackages = "za.co.tms.model")
@ActiveProfiles("test")
public class TenantRepositoryTest {

    private static final String TENANT_NAME = "Tshepo";
    private static final String TENANT_SURNAME = "Mokgoatjane";
    private static final String TENANT_EMAIL = "dummy@email.com";
    private static final String CELLPHONE_NUMBER = "0123456789";
    private static final String ALTERNATIVE_CELLPHONE_NUMBER = "0987654321";
    private static final int NUMBER_OF_TENANTS_IN_UNIT = 1;


    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Should find tenant by name ignoring case")
    void shouldFindByNameIgnoreCase() {
        // Given
        Tenant tenant = new Tenant();

        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setCellPhoneNumber(CELLPHONE_NUMBER);
        tenant.setAlternativeCellPhoneNumber(ALTERNATIVE_CELLPHONE_NUMBER);
        tenant.setRoomNumber(Room.A11);
        tenant.setNumberOfTenantsInUnit(NUMBER_OF_TENANTS_IN_UNIT);
        tenant.setPaymentDay(PaymentDay.DAY_1);
        tenant.setEmail(TENANT_EMAIL);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(5000));
        tenant.setTenantBehaviour(TenantBehaviour.GOOD);

        tenantRepository.save(tenant);

        // When
        Optional<Tenant> found = tenantRepository.findByNameIgnoreCase("tshepo");

        // Then
        assertTrue(found.isPresent());
        assertEquals(TENANT_NAME, found.get().getName());
    }

    @Test
    @DisplayName("Should return empty when tenant name not found")
    void shouldReturnEmptyWhenNameNotFound() {
        Optional<Tenant> found = tenantRepository.findByNameIgnoreCase("nonexistent");
        assertTrue(found.isEmpty());
    }
}