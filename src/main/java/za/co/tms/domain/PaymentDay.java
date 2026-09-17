package za.co.tms.domain;

import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

@Getter
public enum PaymentDay {
    DAY_1(1),
    DAY_5(5),
    DAY_7(7),
    DAY_10(10),
    DAY_15(15),
    DAY_20(20),
    DAY_25(25),
    DAY_30(30),
    LAST_DAY(-1); // -1 represents last day of the month

    private final int day;

    PaymentDay(int day) {
        this.day = day;
    }

    @Override
    public String toString() {
        return day == -1 ? "Last Day of the Month" : "Day " + day;
    }

    public boolean matches(LocalDate date) {
        return date.equals(resolveDueDate(YearMonth.from(date)));
    }

    /**
     * Resolves the actual calendar due date for a given month, clamping the elected
     * day to the number of days in that month. This handles short months correctly:
     * e.g. DAY_30 in February resolves to 28 (or 29 in a leap year), and LAST_DAY
     * always resolves to the final day of the month.
     */
    public LocalDate resolveDueDate(YearMonth yearMonth) {
        int lastDay = yearMonth.lengthOfMonth();
        int targetDay = (this == LAST_DAY) ? lastDay : Math.min(day, lastDay);
        return yearMonth.atDay(targetDay);
    }

    public String getLabel() {
        if (this == LAST_DAY) {
            return "last day of the month";
        }
        return getOrdinalSuffix(day);
    }

    private static String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}