package za.co.tms.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rent payment status for a tenant's current billing period.
 * Used by the tenant landing page to decide whether to show the overdue banner.
 */
@Schema(description = "Current-period rent payment status for a tenant")
public class RentStatusDTO {

    @Schema(description = "Whether rent for the current period has been paid", example = "false")
    private boolean paid;

    @Schema(description = "The due date of the current billing period", example = "2026-02-28")
    private LocalDate dueDate;

    @Schema(description = "Number of days since the due date (0 if not yet due or paid)", example = "5")
    private long daysOverdue;

    @Schema(description = "Whether the persistent overdue banner should be shown (unpaid and past the grace period)", example = "true")
    private boolean showBanner;

    @Schema(description = "Grace period in days before the overdue banner is shown", example = "3")
    private int gracePeriodDays;

    @Schema(description = "Monthly rental amount due", example = "6000.00")
    private BigDecimal amountDue;

    public RentStatusDTO() {
    }

    public RentStatusDTO(boolean paid, LocalDate dueDate, long daysOverdue, boolean showBanner,
                         int gracePeriodDays, BigDecimal amountDue) {
        this.paid = paid;
        this.dueDate = dueDate;
        this.daysOverdue = daysOverdue;
        this.showBanner = showBanner;
        this.gracePeriodDays = gracePeriodDays;
        this.amountDue = amountDue;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(long daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public boolean isShowBanner() {
        return showBanner;
    }

    public void setShowBanner(boolean showBanner) {
        this.showBanner = showBanner;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }
}
