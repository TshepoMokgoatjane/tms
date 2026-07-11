package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.tms.dto.RevenueDTO;
import za.co.tms.service.RevenueService;

@RestController
@RequestMapping("/api/revenue")
@Tag(name = "Revenue Controller", description = "Endpoints for revenue and income tracking")
public class RevenueController {

    private final RevenueService revenueService;

    @Autowired
    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping
    @Operation(summary = "Get revenue data", description = "Returns monthly income, collection rates, YTD totals, and revenue by room")
    public ResponseEntity<RevenueDTO> getRevenueData() {
        return ResponseEntity.ok(revenueService.getRevenueData());
    }
}
