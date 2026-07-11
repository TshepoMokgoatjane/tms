package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.Payment;
import za.co.tms.domain.PaymentMethod;
import za.co.tms.domain.PaymentStatus;
import za.co.tms.dto.ArrearsReportDTO;
import za.co.tms.service.ReportService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report Controller", description = "Endpoints for generating reports")
public class ReportController {

	private final ReportService reportService;

	@Autowired
	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/payments")
	@Operation(summary = "Payment Report", description = "Returns payments filtered by date range, status, method, and tenant")
	public ResponseEntity<List<Payment>> getPaymentReport(
			@Parameter(description = "Start date (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

			@Parameter(description = "End date (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

			@Parameter(description = "Payment status filter")
			@RequestParam(required = false) PaymentStatus status,

			@Parameter(description = "Payment method filter")
			@RequestParam(required = false) PaymentMethod method,

			@Parameter(description = "Filter by specific tenant ID")
			@RequestParam(required = false) Long tenantId) {

		List<Payment> report = reportService.getPaymentReport(startDate, endDate, status, method, tenantId);
		return ResponseEntity.ok(report);
	}

	@GetMapping("/arrears")
	@Operation(summary = "Arrears Report", description = "Returns tenants with outstanding balances sorted by amount owing")
	public ResponseEntity<List<ArrearsReportDTO>> getArrearsReport() {
		List<ArrearsReportDTO> report = reportService.getArrearsReport();
		return ResponseEntity.ok(report);
	}
}
