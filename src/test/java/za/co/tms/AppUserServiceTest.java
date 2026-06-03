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
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.service.AppUserService;

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
}
