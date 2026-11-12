package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.Property;
import za.co.tms.dto.PropertyDTO;
import za.co.tms.service.PropertyService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyController.class);

    private final PropertyService propertyService;

    @Autowired
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping("/all")
    @Operation(summary = "List all properties", description = "Returns metadata for all properties (no lease PDF binary)")
    public ResponseEntity<List<PropertyDTO>> getAllProperties() {
        List<PropertyDTO> dtos = propertyService.findAllMetadata().stream()
                .map(PropertyDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a property", description = "Creates a new property (Admin only)")
    public ResponseEntity<PropertyDTO> createProperty(@RequestBody Property property) {
        Property saved = propertyService.createProperty(property);
        LOGGER.info("Property created: {}", saved.getStreetAddress());
        return ResponseEntity.ok(new PropertyDTO(saved));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update a property", description = "Updates street address and ERF number only (Admin only)")
    public ResponseEntity<PropertyDTO> updateProperty(@PathVariable Long id, @RequestBody Property property) {
        Property saved = propertyService.updateProperty(id, property);
        return ResponseEntity.ok(new PropertyDTO(saved));
    }

    @PutMapping("/{propertyId}/assign-room/{roomId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Assign a room to a property", description = "Links a room to a property (Admin only)")
    public ResponseEntity<PropertyDTO> assignRoom(@PathVariable Long propertyId, @PathVariable Long roomId) {
        propertyService.assignRoom(propertyId, roomId);
        LOGGER.info("Room ID {} assigned to property ID {}", roomId, propertyId);
        return ResponseEntity.ok(new PropertyDTO(propertyService.findById(propertyId)));
    }

    @PutMapping("/{propertyId}/unassign-room/{roomId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Unassign a room from a property", description = "Unlinks a room from its property (Admin only)")
    public ResponseEntity<PropertyDTO> unassignRoom(@PathVariable Long propertyId, @PathVariable Long roomId) {
        propertyService.unassignRoom(roomId);
        LOGGER.info("Room ID {} unassigned from property ID {}", roomId, propertyId);
        return ResponseEntity.ok(new PropertyDTO(propertyService.findById(propertyId)));
    }

    @PostMapping("/{propertyId}/lease")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upload lease agreement PDF", description = "Upload or replace the shared lease PDF for a property (Admin only)")
    @ApiResponse(responseCode = "200", description = "Lease uploaded successfully")
    public ResponseEntity<?> uploadLease(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file) {

        try {
            Property saved = propertyService.uploadLease(propertyId, file);
            LOGGER.info("Lease uploaded for property ID {}", propertyId);
            return ResponseEntity.ok(new PropertyDTO(saved));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Lease upload validation failed for property {}: {}", propertyId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Failed to read uploaded file for property {}: {}", propertyId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to process file upload.");
        }
    }

    @DeleteMapping("/{propertyId}/lease")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete lease agreement", description = "Removes the lease PDF from a property, keeping the property (Admin only)")
    public ResponseEntity<PropertyDTO> deleteLease(@PathVariable Long propertyId) {
        propertyService.deleteLease(propertyId);
        LOGGER.info("Lease deleted for property ID {}", propertyId);
        return ResponseEntity.ok(new PropertyDTO(propertyService.findById(propertyId)));
    }
}
