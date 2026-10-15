package za.co.tms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.domain.Room;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;
import za.co.tms.repository.RoomRepository;
import za.co.tms.repository.TenantRepository;
import za.co.tms.validator.TenantValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantValidator tenantValidator;

    @Mock
    private RoomRepository roomRepository;

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

        Room room = new Room();
        room.setId(10L);
        room.setRentalAmount(BigDecimal.valueOf(6000));
        updatedTenant.setRoom(room);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(exisitngTenant));
        // The tenant is gaining a room, so the service looks it up to mark it occupied.
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Tenant result = tenantService.updateTenant(tenantId, updatedTenant);

        // Assert
        assertEquals("New Name", result.getName());
        assertEquals(BigDecimal.valueOf(6000), result.getRentalAmount());
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

    // ===================== Re-letting safeguard: detach room on deactivation =====================

    @Test
    void deactivatingTenantViaDelete_freesAndDetachesRoom() {
        // Arrange: an active tenant occupying room A8.
        int tenantId = 1;
        Room room = new Room();
        room.setId(10L);
        room.setCode("A8");
        room.setOccupied(true);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Outgoing");
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setRoom(room);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        tenantService.deleteTenantById(tenantId);

        // Assert: tenant retired, room freed AND detached so A8 is cleanly available again.
        assertEquals(TenantStatus.INACTIVE, tenant.getTenantStatus());
        assertFalse(room.isOccupied(), "Room should be freed on deactivation");
        assertNull(tenant.getRoom(), "Room must be detached from the outgoing tenant");
        verify(roomRepository).save(room);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void deactivatingTenantViaUpdate_freesAndDetachesRoom() {
        // Arrange: existing active tenant in room A8.
        int tenantId = 1;
        Room room = new Room();
        room.setId(10L);
        room.setCode("A8");
        room.setOccupied(true);

        Tenant existing = new Tenant();
        existing.setId(tenantId);
        existing.setName("Outgoing");
        existing.setTenantStatus(TenantStatus.ACTIVE);
        existing.setRoom(room);

        // Incoming update flips the tenant to INACTIVE (still referencing the room).
        Tenant update = new Tenant();
        update.setName("Outgoing");
        update.setSurname("Tenant");
        update.setTenantStatus(TenantStatus.INACTIVE);
        update.setRoom(room);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Tenant result = tenantService.updateTenant(tenantId, update);

        // Assert: room freed and detached from the now-inactive tenant.
        assertEquals(TenantStatus.INACTIVE, result.getTenantStatus());
        assertFalse(room.isOccupied(), "Room should be freed when tenant becomes inactive");
        assertNull(result.getRoom(), "Room must be detached from a deactivated tenant");
        verify(roomRepository).save(room);
    }

    @Test
    void updatingActiveTenant_doesNotDetachRoom() {
        // Guard against over-reach: a normal update of a tenant who STAYS active must keep their
        // room assigned (we only detach on deactivation). Here the tenant keeps the same room.
        int tenantId = 1;
        Room room = new Room();
        room.setId(10L);
        room.setCode("A8");
        room.setOccupied(true);

        Tenant existing = new Tenant();
        existing.setId(tenantId);
        existing.setName("Current");
        existing.setTenantStatus(TenantStatus.ACTIVE);
        existing.setRoom(room);

        Tenant update = new Tenant();
        update.setName("Current Updated");
        update.setSurname("Tenant");
        update.setTenantStatus(TenantStatus.ACTIVE);
        update.setRoom(room);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Room swap branch may look the room up; stub leniently in case it's not reached.
        lenient().when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        Tenant result = tenantService.updateTenant(tenantId, update);

        assertEquals(TenantStatus.ACTIVE, result.getTenantStatus());
        assertNotNull(result.getRoom(), "An active tenant must keep their room assigned");
        assertEquals("A8", result.getRoom().getCode());
    }
}