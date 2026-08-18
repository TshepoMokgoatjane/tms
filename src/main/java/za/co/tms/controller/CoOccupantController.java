package za.co.tms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.CoOccupant;
import za.co.tms.domain.Relationship;
import za.co.tms.service.AppUserService;
import za.co.tms.service.CoOccupantService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/co-occupants")
@RequiredArgsConstructor
public class CoOccupantController {

    private final CoOccupantService coOccupantService;
    private final AppUserService appUserService;

    /**
     * ADMIN: Link an existing registered user as a co-occupant to a primary tenant.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createCoOccupant(@RequestBody Map<String, Object> request) {
        try {
            Integer tenantId = (Integer) request.get("tenantId");
            Integer userId = (Integer) request.get("userId");
            String name = (String) request.get("name");
            String surname = (String) request.get("surname");
            String relationship = (String) request.get("relationship");
            String email = (String) request.get("email");
            String cellPhoneNumber = (String) request.get("cellPhoneNumber");
            String vehicleRegistration = (String) request.get("vehicleRegistration");

            CoOccupant coOccupant = new CoOccupant();
            coOccupant.setName(name);
            coOccupant.setSurname(surname);
            coOccupant.setRelationship(Relationship.valueOf(relationship));
            coOccupant.setEmail(email);
            coOccupant.setCellPhoneNumber(cellPhoneNumber);
            coOccupant.setVehicleRegistration(vehicleRegistration);

            CoOccupant saved = coOccupantService.addCoOccupant(coOccupant, tenantId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("name", saved.getName());
            response.put("surname", saved.getSurname());
            response.put("message", "Co-occupant " + saved.getName() + " " + saved.getSurname() + " linked successfully.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN: Get all registered users who are not yet linked to any tenant (eligible for linking).
     */
    @GetMapping("/unlinked-users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUnlinkedUsers() {
        List<Map<String, Object>> users = appUserService.findUnlinkedUsers().stream()
                .map(user -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", user.getId());
                    item.put("firstName", user.getFirstName());
                    item.put("lastName", user.getLastName());
                    item.put("username", user.getUsername());
                    item.put("email", user.getEmail());
                    item.put("cellPhoneNumber", user.getCellPhoneNumber());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * ADMIN: Get all co-occupants across all tenants.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CoOccupant>> getAllCoOccupants() {
        return ResponseEntity.ok(coOccupantService.getAllCoOccupants());
    }

    /**
     * Get co-occupants for a specific tenant.
     */
    @GetMapping("/by-tenant/{tenantId}")
    public ResponseEntity<List<CoOccupant>> getByTenant(@PathVariable Integer tenantId) {
        return ResponseEntity.ok(coOccupantService.getCoOccupantsByTenant(tenantId));
    }

    /**
     * Get a specific co-occupant by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CoOccupant> getById(@PathVariable Long id) {
        return ResponseEntity.ok(coOccupantService.getById(id));
    }

    /**
     * ADMIN: Update co-occupant details.
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateCoOccupant(@PathVariable Long id, @RequestBody CoOccupant coOccupant) {
        try {
            CoOccupant updated = coOccupantService.updateCoOccupant(id, coOccupant);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN: Deactivate a co-occupant (soft delete).
     */
    @DeleteMapping("/deactivate/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deactivateCoOccupant(@PathVariable Long id) {
        coOccupantService.deactivateCoOccupant(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get available relationship types.
     */
    @GetMapping("/relationships")
    public ResponseEntity<Relationship[]> getRelationships() {
        return ResponseEntity.ok(Relationship.values());
    }
}
