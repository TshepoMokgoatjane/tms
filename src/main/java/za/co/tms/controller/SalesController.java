package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.ContactUs;
import za.co.tms.domain.LeadStatus;
import za.co.tms.domain.Room;
import za.co.tms.domain.Tenant;
import za.co.tms.dto.SalesPipelineDTO;
import za.co.tms.service.SalesService;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales Controller", description = "Endpoints for lead tracking and sales pipeline")
public class SalesController {

    private final SalesService salesService;

    @Autowired
    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/pipeline")
    @Operation(summary = "Get pipeline statistics", description = "Returns summary counts for each lead stage")
    public ResponseEntity<SalesPipelineDTO> getPipelineStats() {
        return ResponseEntity.ok(salesService.getPipelineStats());
    }

    @GetMapping("/inquiries")
    @Operation(summary = "Get all inquiries", description = "Returns all Contact Us submissions ordered by most recent")
    public ResponseEntity<List<ContactUs>> getAllInquiries() {
        return ResponseEntity.ok(salesService.getAllInquiries());
    }

    @GetMapping("/inquiries/status/{status}")
    @Operation(summary = "Get inquiries by status", description = "Filter inquiries by lead status")
    public ResponseEntity<List<ContactUs>> getInquiriesByStatus(@PathVariable LeadStatus status) {
        return ResponseEntity.ok(salesService.getInquiriesByStatus(status));
    }

    @PutMapping("/inquiries/{id}/status")
    @Operation(summary = "Update lead status", description = "Move an inquiry to a new pipeline stage")
    public ResponseEntity<ContactUs> updateLeadStatus(@PathVariable Integer id, @RequestParam LeadStatus status) {
        return ResponseEntity.ok(salesService.updateLeadStatus(id, status));
    }

    @GetMapping("/vacant-rooms")
    @Operation(summary = "Get vacant rooms", description = "Returns rooms available for prospective tenants")
    public ResponseEntity<List<Room>> getVacantRooms() {
        return ResponseEntity.ok(salesService.getVacantRooms());
    }

    @GetMapping("/conversions")
    @Operation(summary = "Get recent conversions", description = "Returns tenants who signed leases in the last 90 days")
    public ResponseEntity<List<Tenant>> getRecentConversions() {
        return ResponseEntity.ok(salesService.getRecentConversions());
    }
}
