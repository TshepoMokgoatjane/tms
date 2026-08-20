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
import za.co.tms.repository.TenantRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;

    @Autowired
    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, TenantRepository tenantRepository) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
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

    public void activateUser(int id) {
        AppUser user = findById(id);
        user.setStatus(Status.OPEN);
        user.setDateModified(LocalDateTime.now());
        appUserRepository.save(user);
        log.info("User {} activated", user.getUsername());
    }

    public AppUser updateRole(int id, UserRoles role) {
        AppUser user = findById(id);
        user.setRole(role);
        user.setDateModified(LocalDateTime.now());
        appUserRepository.save(user);
        log.info("User {} role updated to {}", user.getUsername(), role);
        return user;
    }

    public AppUser linkTenant(int userId, Integer tenantId) {
        AppUser user = findById(userId);
        if (tenantId == null) {
            user.setTenant(null);
            log.info("User {} unlinked from tenant", user.getUsername());
        } else {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

            // Sync contact info from user registration to tenant record
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                tenant.setEmail(user.getEmail());
            }
            if (user.getCellPhoneNumber() != null && !user.getCellPhoneNumber().isBlank()) {
                tenant.setCellPhoneNumber(user.getCellPhoneNumber());
            }
            tenantRepository.save(tenant);

            user.setTenant(tenant);
            log.info("User {} linked to tenant ID {} — email/phone synced", user.getUsername(), tenantId);
        }
        user.setDateModified(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    public void updateProfileImage(String username, byte[] imageData, String contentType) {
        AppUser user = findByUsername(username);
        user.setProfileImage(imageData);
        user.setProfileImageType(contentType);
        user.setDateModified(LocalDateTime.now());
        appUserRepository.save(user);
        log.info("Profile image updated for user {}", username);
    }

    public List<AppUser> findUnlinkedUsers() {
        return appUserRepository.findByRoleAndTenantIsNull(UserRoles.USER);
    }

    public void updateLastLogin(int id) {
        AppUser user = findById(id);
        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);
    }

    /**
     * One-time utility: sync email/phone from AppUser to their linked Tenant for all linked users.
     */
    public int syncAllTenantContacts() {
        List<AppUser> allUsers = appUserRepository.findAll();
        int count = 0;
        for (AppUser user : allUsers) {
            if (user.getTenant() != null) {
                Tenant tenant = tenantRepository.findById(user.getTenant().getId()).orElse(null);
                if (tenant != null) {
                    boolean updated = false;
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        tenant.setEmail(user.getEmail());
                        updated = true;
                    }
                    if (user.getCellPhoneNumber() != null && !user.getCellPhoneNumber().isBlank()) {
                        tenant.setCellPhoneNumber(user.getCellPhoneNumber());
                        updated = true;
                    }
                    if (updated) {
                        tenantRepository.save(tenant);
                        count++;
                        log.info("Synced contact for tenant {} {} from user {}", tenant.getName(), tenant.getSurname(), user.getUsername());
                    }
                }
            }
        }
        log.info("Total tenants synced: {}", count);
        return count;
    }
}
