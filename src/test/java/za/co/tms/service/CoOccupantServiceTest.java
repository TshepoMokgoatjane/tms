package za.co.tms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.CoOccupant;
import za.co.tms.domain.PaymentDay;
import za.co.tms.domain.Relationship;
import za.co.tms.domain.Status;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.repository.CoOccupantRepository;
import za.co.tms.repository.TenantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CoOccupantServiceTest {

    @Mock
    private CoOccupantRepository coOccupantRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AppUserService appUserService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private CoOccupantService coOccupantService;

    private CoOccupant coOccupant(long id, Status status) {
        CoOccupant c = new CoOccupant();
        c.setId(id);
        c.setName("Orphan");
        c.setSurname("Dependent");
        c.setRelationship(Relationship.SIBLING);
        c.setEmail("orphan" + id + "@testemail.com");
        c.setStatus(status);
        return c;
    }

    private AppUser coOccupantUser(int id, Status status) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername("orphan-login-" + id);
        u.setRole(UserRoles.CO_OCCUPANT);
        u.setStatus(status);
        return u;
    }

    @Test
    void reconcile_closesOrphanRecordsAndLoginAccounts() {
        CoOccupant orphan1 = coOccupant(1L, Status.OPEN);
        CoOccupant orphan2 = coOccupant(2L, Status.OPEN);
        AppUser orphanLogin = coOccupantUser(50, Status.OPEN);

        when(coOccupantRepository.findActiveWithTenantStatus(Status.CLOSED, TenantStatus.INACTIVE))
                .thenReturn(List.of(orphan1, orphan2));
        when(appUserRepository.findActiveByRoleWithTenantStatus(UserRoles.CO_OCCUPANT, Status.CLOSED, TenantStatus.INACTIVE))
                .thenReturn(List.of(orphanLogin));

        Map<String, Integer> summary = coOccupantService.reconcileOrphanedCoOccupants();

        assertEquals(Status.CLOSED, orphan1.getStatus());
        assertEquals(Status.CLOSED, orphan2.getStatus());
        assertEquals(Status.CLOSED, orphanLogin.getStatus());
        assertEquals(2, summary.get("coOccupantsClosed"));
        assertEquals(1, summary.get("loginAccountsClosed"));
        verify(coOccupantRepository).saveAll(List.of(orphan1, orphan2));
        verify(appUserRepository).saveAll(List.of(orphanLogin));
    }

    @Test
    void reconcile_whenNothingOrphaned_closesNothing() {
        when(coOccupantRepository.findActiveWithTenantStatus(Status.CLOSED, TenantStatus.INACTIVE))
                .thenReturn(List.of());
        when(appUserRepository.findActiveByRoleWithTenantStatus(UserRoles.CO_OCCUPANT, Status.CLOSED, TenantStatus.INACTIVE))
                .thenReturn(List.of());

        Map<String, Integer> summary = coOccupantService.reconcileOrphanedCoOccupants();

        assertEquals(0, summary.get("coOccupantsClosed"));
        assertEquals(0, summary.get("loginAccountsClosed"));
        verify(coOccupantRepository, never()).saveAll(any());
        verify(appUserRepository, never()).saveAll(any());
    }

    // ===================== Promote co-occupant to primary tenant =====================

    @Test
    void promote_createsTenant_relinksLogin_andClosesCoOccupant() {
        CoOccupant co = coOccupant(7L, Status.OPEN);
        co.setName("Zizo");
        co.setSurname("Ngcingane");
        co.setEmail("zizo@testemail.com");

        AppUser loginAccount = coOccupantUser(90, Status.OPEN);
        loginAccount.setEmail("zizo@testemail.com");

        Tenant createdTenant = new Tenant();
        createdTenant.setId(42);
        createdTenant.setName("Zizo");
        createdTenant.setSurname("Ngcingane");

        when(coOccupantRepository.findById(7L)).thenReturn(Optional.of(co));
        when(tenantService.addTenant(any(Tenant.class))).thenReturn(createdTenant);
        when(appUserRepository.findByEmail("zizo@testemail.com")).thenReturn(Optional.of(loginAccount));

        Tenant result = coOccupantService.promoteToPrimaryTenant(
                7L, 10L, LocalDate.now(), LocalDate.now().plusMonths(12), PaymentDay.DAY_1, null);

        assertEquals(42, result.getId());
        // Login account re-linked to the new tenant and upgraded to TENANT.
        verify(appUserService).linkTenant(90, 42);
        verify(appUserService).updateRole(90, UserRoles.TENANT);
        // Co-occupant record closed.
        assertEquals(Status.CLOSED, co.getStatus());
        verify(coOccupantRepository).save(co);
    }

    @Test
    void promote_withoutPaymentDay_throws() {
        CoOccupant co = coOccupant(7L, Status.OPEN);
        when(coOccupantRepository.findById(7L)).thenReturn(Optional.of(co));

        assertThrows(IllegalArgumentException.class, () ->
                coOccupantService.promoteToPrimaryTenant(7L, 10L, null, null, null, null));
    }

    @Test
    void promote_whenCoOccupantAlreadyClosed_throws() {
        CoOccupant co = coOccupant(7L, Status.CLOSED);
        when(coOccupantRepository.findById(7L)).thenReturn(Optional.of(co));

        assertThrows(IllegalArgumentException.class, () ->
                coOccupantService.promoteToPrimaryTenant(7L, 10L, null, null, PaymentDay.DAY_1, null));
    }
}
