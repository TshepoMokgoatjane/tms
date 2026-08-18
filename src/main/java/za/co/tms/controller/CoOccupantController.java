package za.co.tms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.CoOccupant;
import za.co.tms.domain.Relationship;
import za.co.tms.service.CoOccupantService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/co-occupants")
@RequiredArgsConstructor
public class CoOccupantController {

    private final CoOccupantService coOccupantService;

    /**
     * ADMIN: Create a new co-occupant with login credentials.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createCoOccupant(@RequestBody Map<String, Object> request) {
        try {
            Integer tenantId = (Integer) request.get("tenantId");
            String name = (String) request.get("name");
            String surname = (String) request.get("surname");
            String relationship = (String) request.get("relationship");
            String email = (String) request.get("email");
            String cellPhoneNumber = (String) request.get("cellPhoneNumber");
            String vehicleRegistration = (String) request.get("vehicleRegistration");
            String username = (String) request.get("username");
            String tempPassword = (String) request.get("tempPassword");

            CoOccupant coOccupant = new CoOccupant();
            coOccupant.setName(name);
            coOccupant.setSurname(surname);
            coOccupant.setRelationship(Relationship.valueOf(relationship));
            coOccupant.setEmail(email);
            coOccupant.setCellPhoneNumber(cellPhoneNumber);
            coOccupant.setVehicleRegistration(vehicleRegistration);

            CoOccupant saved = coOccupantService.addCoOccupant(coOccupant, tenantId, username, tempPassword);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("name", saved.getName());
            response.put("surname", saved.getSurname());
            response.put("message", "Co-occupant " + saved.getName() + " " + saved.getSurname() + " created successfully with login credentials.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
