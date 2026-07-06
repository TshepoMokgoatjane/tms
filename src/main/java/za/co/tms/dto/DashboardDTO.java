package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    // Summary KPIs
    private int totalTenants;
    private int activeTenants;
    private int inactiveTenants;
    private int totalRooms;
    private int occupiedRooms;
    private int availableRooms;
    private int occupancyRatePercent;
    private BigDecimal monthlyRevenueTarget;
    private BigDecimal collectedThisMonth;
    private int openTickets;
    private int resolvedTickets;

    // Monthly revenue chart data (rental collected per month)
    private List<MonthlyRevenue> monthlyRevenue;

    // Occupancy over time
    private List<OccupancySnapshot> occupancyTrend;

    // Payment status breakdown
    private int paidCount;
    private int pendingCount;
    private int failedCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal collected;
        private BigDecimal target;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupancySnapshot {
        private String month;
        private int occupied;
        private int available;
    }
}
