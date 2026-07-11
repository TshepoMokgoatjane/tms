package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArrearsReportDTO {
	private Integer tenantId;
	private String tenantName;
	private String roomCode;
	private BigDecimal monthlyRent;
	private BigDecimal totalPaid;
	private BigDecimal totalOwing;
	private int monthsInArrears;
	private String tenantBehaviour;
	private String tenantStatus;
}
