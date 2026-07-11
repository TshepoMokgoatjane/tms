package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesPipelineDTO {
 private long totalInquiries;
 private long newLeads;
 private long contacted;
 private long viewingScheduled;
 private long leaseSigned;
 private long lost;
 private long vacantRooms;
 private double conversionRate;
}