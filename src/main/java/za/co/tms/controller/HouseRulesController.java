package za.co.tms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.HouseRules;
import za.co.tms.domain.HouseRulesAcknowledgement;
import za.co.tms.service.HouseRulesService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/house-rules")
@RequiredArgsConstructor
public class HouseRulesController {

    private final HouseRulesService houseRulesService;

    /**
     * ADMIN: Upload a new house rules PDF.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadHouseRules(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are allowed"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        HouseRules saved = houseRulesService.uploadHouseRules(file, description, username);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("version", saved.getVersion());
        response.put("fileName", saved.getFileName());
        response.put("uploadedAt", saved.getUploadedAt());
        response.put("uploadedBy", saved.getUploadedBy());
        response.put("description", saved.getDescription());
        response.put("message", "House Rules v" + saved.getVersion() + " uploaded successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get the current active house rules metadata (no file data).
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentHouseRules() {
        return houseRulesService.getCurrentHouseRules()
                .map(hr -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", hr.getId());
                    response.put("version", hr.getVersion());
                    response.put("fileName", hr.getFileName());
                    response.put("uploadedAt", hr.getUploadedAt());
                    response.put("uploadedBy", hr.getUploadedBy());
                    response.put("description", hr.getDescription());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Download the current active house rules PDF.
     */
    @GetMapping("/current/download")
    public ResponseEntity<byte[]> downloadCurrentHouseRules() {
        return houseRulesService.getCurrentHouseRules()
                .map(hr -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(hr.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + hr.getFileName() + "\"")
                        .body(hr.getFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Download a specific house rules version by ID.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadHouseRulesById(@PathVariable Long id) {
        return houseRulesService.getById(id)
                .map(hr -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(hr.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + hr.getFileName() + "\"")
                        .body(hr.getFileData()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ADMIN: Get all house rules versions (history).
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<Map<String, Object>> history = houseRulesService.getAllVersions().stream()
                .map(hr -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", hr.getId());
                    item.put("version", hr.getVersion());
                    item.put("fileName", hr.getFileName());
                    item.put("uploadedAt", hr.getUploadedAt());
                    item.put("uploadedBy", hr.getUploadedBy());
                    item.put("description", hr.getDescription());
                    item.put("active", hr.isActive());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(history);
    }

    /**
     * TENANT: Acknowledge the current house rules.
     */
    @PostMapping("/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeHouseRules(
            @RequestParam("tenantId") Integer tenantId,
            HttpServletRequest request) {

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        try {
            HouseRulesAcknowledgement ack = houseRulesService.acknowledge(tenantId, ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "House Rules acknowledged successfully");
            response.put("acknowledgedAt", ack.getAcknowledgedAt());
            response.put("version", ack.getHouseRules().getVersion());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * TENANT: Check if tenant has acknowledged the current house rules.
     */
    @GetMapping("/acknowledgement-status/{tenantId}")
    public ResponseEntity<Map<String, Object>> getAcknowledgementStatus(@PathVariable Integer tenantId) {
        boolean acknowledged = houseRulesService.hasAcknowledgedCurrent(tenantId);
        Map<String, Object> response = new HashMap<>();
        response.put("acknowledged", acknowledged);

        houseRulesService.getCurrentHouseRules().ifPresent(hr -> {
            response.put("currentVersion", hr.getVersion());
            response.put("houseRulesId", hr.getId());
        });

        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN: Get all acknowledgements for a specific house rules version.
     */
    @GetMapping("/{id}/acknowledgements")
    public ResponseEntity<List<Map<String, Object>>> getAcknowledgements(@PathVariable Long id) {
        List<Map<String, Object>> acknowledgements = houseRulesService.getAcknowledgementsForVersion(id).stream()
                .map(ack -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tenantId", ack.getTenant().getId());
                    item.put("tenantName", ack.getTenant().getName() + " " + ack.getTenant().getSurname());
                    item.put("room", ack.getTenant().getRoom() != null ? ack.getTenant().getRoom().getCode() : "N/A");
                    item.put("acknowledgedAt", ack.getAcknowledgedAt());
                    item.put("ipAddress", ack.getIpAddress());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(acknowledgements);
    }
}
