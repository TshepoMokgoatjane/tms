package za.co.tms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.tms.model.UserInfo;
import za.co.tms.repository.UserInfoRepository;
import za.co.tms.service.UserInfoService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserInfoServiceTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserInfoService userInfoService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("tshepo");
        userInfo.setPassword("encodedPass");
        userInfo.setRoles("ROLE_USER");

        when(userInfoRepository.findByUsername("tshepo")).thenReturn(Optional.of(userInfo));

        UserDetails result = userInfoService.loadUserByUsername("tshepo");

        assertEquals("tshepo", result.getUsername());
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionIfUserNotFound() {
        when(userInfoRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userInfoService.loadUserByUsername("unknown"));
    }
}