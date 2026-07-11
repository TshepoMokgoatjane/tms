package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.*;
import za.co.tms.dto.ArrearsReportDTO;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.TenantRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportService.class);

	private final PaymentRepository paymentRepository;
	private final TenantRepository tenantRepository;

	@Autowired
	public ReportService(PaymentRepository paymentRepository, TenantRepository tenantRepository) {
		this.paymentRepository = paymentRepository;
		this.tenantRepository = tenantRepository;
	}

	/**
	 * Payment Report: returns payments filtered by date range, status, method, and/or tenant.
	 */
	public List<Payment> getPaymentReport(LocalDateTime startDate, LocalDateTime endDate,
			PaymentStatus status, PaymentMethod method, Long tenantId) {
		LOGGER.info("Generating payment report: start={}, end={}, status={}, method={}, tenantId={}",
			startDate, endDate, status, method, tenantId);

		List<Payment> payments;

		// Start with date range (always required for reports)
		if (startDate != null && endDate != null) {
			if (status != null && method != null) {
				payments = paymentRepository.findByPaymentDateBetweenAndPaymentStatusAndPaymentMethod(startDate, endDate, status, method);
			} else if (status != null) {
				payments = paymentRepository.findByPaymentDateBetweenAndPaymentStatus(startDate, endDate, status);
			} else if (method != null) {
				payments = paymentRepository.findByPaymentDateBetweenAndPaymentMethod(startDate, endDate, method);
			} else {
				payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
			}
		} else {
			payments = paymentRepository.findAll();
		}

		// Further filter by tenant if specified
		if (tenantId != null) {
			payments = payments.stream()
				.filter(p -> p.getTenant() != null && p.getTenant().getId() != null
						&& p.getTenant().getId().longValue() == tenantId)
				.collect(Collectors.toList());
		}

		LOGGER.info("Payment report generated: {} records", payments.size());
		return payments;
	}

	/**
	 * Arrears Report: calculates outstanding balances for active tenants.
	 * Arrears = (months since lease start * monthly rent) - total PAID payments.
	 */
	public List<ArrearsReportDTO> getArrearsReport() {
		LOGGER.info("Generating arrears report");

		List<Tenant> activeTenants = tenantRepository.findAll().stream()
			.filter(t -> t.getTenantStatus() == TenantStatus.ACTIVE)
			.collect(Collectors.toList());

		List<ArrearsReportDTO> report = new ArrayList<>();

		for (Tenant tenant : activeTenants) {
			if (tenant.getRentalAmount() == null || tenant.getLeaseStartDate() == null) {
				continue;
			}

			// Calculate months since lease started
			long monthsSinceStart = ChronoUnit.MONTHS.between(tenant.getLeaseStartDate(), LocalDate.now());
			if (monthsSinceStart <= 0) {
				continue;
			}

			// Total expected rent
			BigDecimal expectedTotal = tenant.getRentalAmount().multiply(BigDecimal.valueOf(monthsSinceStart));

			// Total actually paid
			List<Payment> paidPayments = paymentRepository.findByTenantId(tenant.getId().longValue()).stream()
				.filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
				.collect(Collectors.toList());

			BigDecimal totalPaid = paidPayments.stream()
				.map(Payment::getAmount)
				.filter(a -> a != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal owing = expectedTotal.subtract(totalPaid);

			// Only include tenants who actually owe money
			if (owing.compareTo(BigDecimal.ZERO) > 0) {
				int monthsInArrears = tenant.getRentalAmount().compareTo(BigDecimal.ZERO) > 0
					? owing.divide(tenant.getRentalAmount(), 0, java.math.RoundingMode.CEILING).intValue()
					: 0;

				String roomCode = tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A";

				ArrearsReportDTO dto = new ArrearsReportDTO(
					tenant.getId(),
					tenant.getName() + " " + tenant.getSurname(),
					roomCode,
					tenant.getRentalAmount(),
					totalPaid,
					owing,
					monthsInArrears,
					tenant.getTenantBehaviour() != null ? tenant.getTenantBehaviour().name() : "N/A",
					tenant.getTenantStatus().name()
				);

				report.add(dto);
			}
		}

		// Sort by amount owing descending (worst offenders first)
		report.sort((a, b) -> b.getTotalOwing().compareTo(a.getTotalOwing()));

		LOGGER.info("Arrears report generated: {} tenants in arrears", report.size());
		return report;
	}
}
