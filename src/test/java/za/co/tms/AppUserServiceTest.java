package za.co.tms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.Status;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.TenantStatus;
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.repository.TenantRepository;
import za.co.tms.service.AppUserService;
import za.co.tms.service.TenantService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        AppUser appUser = new AppUser();
        appUser.setUsername("tshepo");
        appUser.setPassword("encodedPass");
        appUser.setRole(UserRoles.USER);
        appUser.setStatus(Status.OPEN);

        when(appUserRepository.findByUsername("tshepo")).thenReturn(Optional.of(appUser));

        UserDetails result = appUserService.loadUserByUsername("tshepo");

        assertEquals("tshepo", result.getUsername());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionIfUserNotFound() {
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> appUserService.loadUserByUsername("unknown"));
    }

    @Test
    void registerUser_shouldEncodePasswordAndSaveUser() {
        AppUser appUser = new AppUser();
        appUser.setUsername("tshepo");
        appUser.setPassword("plain123");
        appUser.setEmail("tshepo@test.com");
        appUser.setRole(UserRoles.USER);

        when(passwordEncoder.encode("plain123")).thenReturn("encoded123");
        when(appUserRepository.existsByUsername("tshepo")).thenReturn(false);
        when(appUserRepository.existsByEmail("tshepo@test.com")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUser result = appUserService.registerUser(appUser);

        assertEquals("encoded123", result.getPassword());
        assertEquals(Status.OPEN, result.getStatus());
        assertNotNull(result.getDateCreated());
        verify(appUserRepository).save(appUser);
    }

    // ===================== Deactivation sync: account -> linked tenant =====================

    @Test
    void deactivateUser_withLinkedActiveTenant_retiresTenantToo() {
        // Arrange: a tenant account linked to an ACTIVE tenant record.
        Tenant tenant = new Tenant();
        tenant.setId(7);
        tenant.setTenantStatus(TenantStatus.ACTIVE);

        AppUser user = new AppUser();
        user.setId(1);
        user.setUsername("mary");
        user.setRole(UserRoles.TENANT);
        user.setStatus(Status.OPEN);
        user.setTenant(tenant);

        when(appUserRepository.findById(1)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        appUserService.deactivateUser(1);

        // Assert: account closed AND the linked tenant retired via the tested path.
        assertEquals(Status.CLOSED, user.getStatus());
        verify(appUserRepository).save(user);
        verify(tenantService).deleteTenantById(7);
    }

    @Test
    void deactivateUser_withNoLinkedTenant_doesNotTouchTenantService() {
        // Arrange: an admin account with no tenant link.
        AppUser admin = new AppUser();
        admin.setId(2);
        admin.setUsername("boss");
        admin.setRole(UserRoles.ADMIN);
        admin.setStatus(Status.OPEN);
        admin.setTenant(null);

        when(appUserRepository.findById(2)).thenReturn(Optional.of(admin));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        appUserService.deactivateUser(2);

        // Assert: account closed, tenant side never involved.
        assertEquals(Status.CLOSED, admin.getStatus());
        verify(appUserRepository).save(admin);
        verifyNoInteractions(tenantService);
    }

    @Test
    void deactivateUser_withAlreadyInactiveTenant_doesNotRetireAgain() {
        // Arrange: linked tenant is already INACTIVE — nothing more to do on the tenant side.
        Tenant tenant = new Tenant();
        tenant.setId(9);
        tenant.setTenantStatus(TenantStatus.INACTIVE);

        AppUser user = new AppUser();
        user.setId(3);
        user.setUsername("former");
        user.setRole(UserRoles.TENANT);
        user.setStatus(Status.OPEN);
        user.setTenant(tenant);

        when(appUserRepository.findById(3)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        appUserService.deactivateUser(3);

        // Assert: account closed; tenant retirement not re-invoked (idempotent guard).
        assertEquals(Status.CLOSED, user.getStatus());
        verify(tenantService, never()).deleteTenantById(anyInt());
    }
}
