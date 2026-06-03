package za.co.tms.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import za.co.tms.domain.AppUser;
import za.co.tms.domain.UserRoles;
import za.co.tms.dto.AuthResponse;
import za.co.tms.dto.RegisterRequest;
import za.co.tms.jwt.JwtTokenRequest;
import za.co.tms.jwt.JwtTokenService;
import za.co.tms.service.AppUserService;

import java.util.List;

@RestController
@RequestMapping(path = "/auth")
public class UserController {

    private final AppUserService appUserService;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserController(AppUserService appUserService, JwtTokenService jwtTokenService, AuthenticationManager authenticationManager) {
        this.appUserService = appUserService;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/generateToken")
    public ResponseEntity<AuthResponse> authenticateAndGetToken(@RequestBody JwtTokenRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));

        if (authentication.isAuthenticated()) {
            AppUser user = appUserService.findByUsername(authRequest.username());
            String token = jwtTokenService.generateToken(authRequest.username());
            AuthResponse response = new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getRole().name(),
                    user.getFirstName(),
                    user.getLastName()
            );
            return ResponseEntity.ok(response);
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest request) {
        AppUser appUser = new AppUser();
        appUser.setFirstName(request.getFirstName());
        appUser.setLastName(request.getLastName());
        appUser.setUsername(request.getUsername());
        appUser.setEmail(request.getEmail());
        appUser.setCellPhoneNumber(request.getCellPhoneNumber());
        appUser.setPassword(request.getPassword());

        // Set role (default to USER if not specified)
        if (request.getRole() != null && !request.getRole().isBlank()) {
            appUser.setRole(UserRoles.valueOf(request.getRole().toUpperCase()));
        } else {
            appUser.setRole(UserRoles.USER);
        }

        appUserService.registerUser(appUser);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    // Keep backward compatibility with old endpoint
    @PostMapping("/addNewUser")
    public ResponseEntity<String> addNewUser(@Valid @RequestBody RegisterRequest request) {
        return registerUser(request);
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_TENANT')")
    public ResponseEntity<AppUser> getUserProfile(Authentication authentication) {
        AppUser user = appUserService.findByUsername(authentication.getName());
        user.setPassword(null); // Don't expose password
        return ResponseEntity.ok(user);
    }

    @PutMapping("/user/profile")
    @PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_TENANT')")
    public ResponseEntity<AppUser> updateUserProfile(Authentication authentication, @RequestBody AppUser updatedUser) {
        AppUser currentUser = appUserService.findByUsername(authentication.getName());
        AppUser updated = appUserService.updateProfile(currentUser.getId(), updatedUser);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AppUser>> getAllUsers() {
        List<AppUser> users = appUserService.findAllUsers();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable int id) {
        appUserService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to the public endpoint!";
    }
}
