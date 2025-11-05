package za.co.tms.model;

import java.time.LocalDate;

public enum PaymentDay {
    DAY_1(1),
    DAY_5(5),
    DAY_7(7),
    DAY_15(15),
    DAY_20(20),
    DAY_25(25),
    DAY_30(30),
    LAST_DAY(-1); // -1 represents last day of the month

    private final int day;

    PaymentDay(int day) {
        this.day = day;
    }

    public int getDay() {
        return this.day;
    }

    @Override
    public String toString() {
        return day == -1 ? "Last Day of the Month" : "Day " + day;
    }

    public boolean matches(LocalDate date) {
        if (this == LAST_DAY) {
            return date.getDayOfMonth() == date.lengthOfMonth();
        }
        return date.getDayOfMonth() == day;
    }

    public String getLabel() {
        return this == LAST_DAY ? "Last Day" : String.valueOf(day);
    }
}