package za.co.tms.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Schema(description = "Tenant entity representing a person renting a unit")
@Entity
public class Tenant extends AuditModel implements Serializable {
    
	/**
	 * 
	 */
	@Serial
    private static final long serialVersionUID = 7011620956908743611L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    @Schema(description = "Unique identifier of the tenant", example = "1")
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    @Schema(description = "Tenant's first name", example = "James")
    private String name;

    @NotNull
    @Size(min = 1, max = 50)
    @Schema(description = "Tenant's surname", example = "Bond")
    private String surname;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Tenant's title", example = "MR")
    private Title title;

    @Email
    @Schema(description = "Tenant's email address", example = "james.bond@testemail.com")
    private String email;

    @Size(max = 10)
    @Schema(description = "Primary cell phone number", example = "0821234567")
    private String cellPhoneNumber;

    @Size(max = 10)
    @Schema(description = "Alternative cell phone number", example = "0839876543")
    private String alternativeCellPhoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "room_number")
    @Schema(description = "Room assigned to the tenant", example = "A1")
    private Room roomNumber;

    @Min(1)
    @Schema(description = "Number of tenants in the unit", example = "2")
    private int numberOfTenantsInUnit;

    @PastOrPresent
    @Schema(description = "Lease start date", example = "2025-11-01")
    private LocalDate leaseStartDate;

    @FutureOrPresent
    @Schema(description = "Lease end date", example = "2026-11-01")
    private LocalDate leaseEndDate;

    @Schema(description = "Indicates if deposit has been paid", example = "true")
    private boolean depositPaid;

    @Column(precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Rental must be zero or positive")
    @Schema(description = "Monthly rental amount", example = "6000.00")
    private BigDecimal rental;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(description = "Date of rent payment", example = "25th")
    private PaymentDay paymentDay;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(description = "Tenant's behaviour rating", example = "GOOD")
    private TenantBehaviour tenantBehaviour;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(description = "Tenant's current status", example = "ACTIVE")
    private TenantStatus tenantStatus;
}