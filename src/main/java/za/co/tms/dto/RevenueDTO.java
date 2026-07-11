package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDTO {

    private BigDecimal thisMonthIncome;
    private BigDecimal lastMonthIncome;
    private BigDecimal yearToDateIncome;
    private BigDecimal monthlyTarget;
    private double collectionRatePercent;

    private List<MonthlyData> monthlyIncome;
    private List<RoomRevenue> revenueByRoom;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;
        private int year;
        private BigDecimal collected;
        private BigDecimal expected;
        private double collectionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomRevenue {
        private String roomCode;
        private String description;
        private BigDecimal monthlyRent;
        private BigDecimal totalCollected;
        private boolean occupied;
    }
}
