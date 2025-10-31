package za.co.tms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantStatus;
import za.co.tms.repository.TenantRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldReturnAllTenants() {
        Tenant tenant = new Tenant();
        tenant.setName("Tshepo");

        when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        List<Tenant> tenants = tenantService.findAllTenants();

        assertEquals(1, tenants.size());
        assertEquals("Tshepo", tenants.get(0).getName());
    }

    @Test
    void shouldReturnTenantByName() {
        String name = "Tshepo";
        Tenant tenant = new Tenant();
        tenant.setName(name);

        when(tenantRepository.findByNameIgnoreCase(name)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.findTenantByName(name);

        assertEquals(name, result.getName());
    }

    @Test
    void shouldThrowExceptionWhenTenantNotFoundByName() {
        String name = "unknown";

        when(tenantRepository.findByNameIgnoreCase(name)).thenReturn(Optional.empty());

        assertThrows(TenantNotFoundException.class, () -> tenantService.findTenantByName(name));
    }

    @Test
    void shouldReturnTenantById() {
        int tenantId = 1;
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.findTenantById(tenantId);

        assertEquals(tenantId, result.getId());
    }

    @Test
    void shouldThrowExceptionWhenTenantNotFoundById() {
        int tenantId = 999;

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(TenantNotFoundException.class, () -> tenantService.findTenantById(tenantId));
    }

    @Test
    void shouldAddTenantSuccessfully() {
        Tenant tenant = new Tenant();
        tenant.setName("New Tenant");

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant result = tenantService.addTenant(tenant);

        assertNull(result.getId()); // assuming it is set to null before safe

        assertEquals("New Tenant", result.getName());
    }

    @Test
    void shouldUpdateTenantSuccessfully() {
        // Arrange
        int tenantId = 1;

        Tenant exisitngTenant = new Tenant();
        exisitngTenant.setId(tenantId);
        exisitngTenant.setName("Old Name");
        exisitngTenant.setSurname("Tenant");
        exisitngTenant.setTenantStatus(TenantStatus.ACTIVE);

        Tenant updatedTenant = new Tenant();
        updatedTenant.setName("New Name");
        updatedTenant.setSurname("Tenant");
        updatedTenant.setTenantStatus(TenantStatus.ACTIVE);
        updatedTenant.setRental(BigDecimal.valueOf(6000));

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(exisitngTenant));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Tenant result = tenantService.updateTenant(tenantId, updatedTenant);

        // Assert
        assertEquals("New Name", result.getName());
        assertEquals(BigDecimal.valueOf(6000), result.getRental());
        verify(tenantRepository).findById(tenantId);
        verify(tenantRepository).save(exisitngTenant);
    }

    @Test
    void shouldDeleteTenantByIdSuccessfully() {
        // Arrange
        int tenantId = 1;

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);;
        tenant.setTenantStatus(TenantStatus.ACTIVE);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tenantService.deleteTenantById(tenantId);

        // Assert
        assertEquals(TenantStatus.INACTIVE, tenant.getTenantStatus());
        verify(tenantRepository).findById(tenantId);
        verify(tenantRepository).save(tenant);
    }
}