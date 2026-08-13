package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.LeaseAgreement;
import za.co.tms.dto.LeaseAgreementDTO;
import za.co.tms.service.LeaseAgreementService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lease-agreements")
public class LeaseAgreementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaseAgreementController.class);

    private final LeaseAgreementService leaseAgreementService;

    @Autowired
    public LeaseAgreementController(LeaseAgreementService leaseAgreementService) {
        this.leaseAgreementService = leaseAgreementService;
    }

    // ========== ADMIN ENDPOINTS ==========

    @PostMapping("/upload/{tenantId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Upload lease agreement PDF", description = "Upload or replace a lease agreement for a tenant (Admin only)")
    @ApiResponse(responseCode = "200", description = "Lease agreement uploaded successfully")
    public ResponseEntity<?> uploadLeaseAgreement(
            @PathVariable Integer tenantId,
            @RequestParam("file") MultipartFile file) {

        try {
            LeaseAgreement saved = leaseAgreementService.uploadLeaseAgreement(tenantId, file);
            LOGGER.info("Lease agreement uploaded for tenant ID {}", tenantId);
            return ResponseEntity.ok(new LeaseAgreementDTO(saved));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Lease upload validation failed for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Failed to read uploaded file for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to process file upload.");
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "List all lease agreements", description = "Returns metadata for all uploaded lease agreements (Admin only)")
    public ResponseEntity<List<LeaseAgreementDTO>> getAllLeaseAgreements() {
        List<LeaseAgreement> leases = leaseAgreementService.getAllLeaseAgreements();
        List<LeaseAgreementDTO> dtos = leases.stream()
                .map(LeaseAgreementDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete lease agreement", description = "Permanently removes the lease agreement for a tenant (Admin only)")
    @ApiResponse(responseCode = "204", description = "Lease agreement deleted")
    public ResponseEntity<Void> deleteLeaseAgreement(@PathVariable Integer tenantId) {
        leaseAgreementService.deleteLeaseAgreement(tenantId);
        LOGGER.info("Lease agreement deleted for tenant ID {}", tenantId);
        return ResponseEntity.noContent().build();
    }

    // ========== SHARED ENDPOINTS (Admin & Tenant) ==========

    @GetMapping("/check/{tenantId}")
    @Operation(summary = "Check lease agreement existence", description = "Returns metadata if a lease exists for the tenant")
    public ResponseEntity<LeaseAgreementDTO> checkLeaseAgreement(@PathVariable Integer tenantId) {
        if (!leaseAgreementService.existsForTenant(tenantId)) {
            return ResponseEntity.ok(LeaseAgreementDTO.notFound(tenantId));
        }

        LeaseAgreement lease = leaseAgreementService.getLeaseAgreementByTenantId(tenantId);
        return ResponseEntity.ok(new LeaseAgreementDTO(lease));
    }

    @GetMapping("/download/{tenantId}")
    @Operation(summary = "Download lease agreement PDF", description = "Returns the PDF binary for the tenant's lease agreement")
    public ResponseEntity<byte[]> downloadLeaseAgreement(@PathVariable Integer tenantId) {
        LeaseAgreement lease = leaseAgreementService.getLeaseAgreementByTenantId(tenantId);

        if (lease.getPdfData() == null || lease.getPdfData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + lease.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(lease.getFileSize()))
                .body(lease.getPdfData());
    }
}
