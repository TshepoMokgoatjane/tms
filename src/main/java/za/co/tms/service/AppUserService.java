package za.co.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.tms.domain.*;
import za.co.tms.repository.AppUserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new AppUserDetails(user);
    }

    public AppUser registerUser(AppUser appUser) {
        if (appUserRepository.existsByUsername(appUser.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + appUser.getUsername());
        }
        if (appUserRepository.existsByEmail(appUser.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + appUser.getEmail());
        }

        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
        appUser.setDateCreated(LocalDateTime.now());
        appUser.setDateModified(LocalDateTime.now());
        appUser.setStatus(Status.OPEN);

        if (appUser.getRole() == null) {
            appUser.setRole(UserRoles.USER);
        }

        log.info("Registering new user: {} with role: {}", appUser.getUsername(), appUser.getRole());
        return appUserRepository.save(appUser);
    }

    public AppUser findById(int id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User with ID " + id + " not found"));
    }

    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public AppUser findByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));
    }

    public List<AppUser> findAllUsers() {
        return appUserRepository.findAll();
    }

    public AppUser updateProfile(int id, AppUser updatedUser) {
        AppUser existing = findById(id);
        existing.setFirstName(updatedUser.getFirstName());
        existing.setLastName(updatedUser.getLastName());
        existing.setEmail(updatedUser.getEmail());
        existing.setCellPhoneNumber(updatedUser.getCellPhoneNumber());
        existing.setDateModified(LocalDateTime.now());
        return appUserRepository.save(existing);
    }

    public void deactivateUser(int id) {
        AppUser user = findById(id);
        user.setStatus(Status.CLOSED);
        user.setDateModified(LocalDateTime.now());
        appUserRepository.save(user);
        log.info("User {} deactivated", user.getUsername());
    }
}
