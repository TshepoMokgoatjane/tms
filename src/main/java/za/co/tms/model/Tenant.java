package za.co.tms.model;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
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
    private Integer id;

    @NotNull
    @Size(min = 1, max = 50)
    private String name;

    @NotNull
    @Size(min = 1, max = 50)
    private String surname;

    private String title;

    @Email
    private String email;

    @Size(max = 10)
    private String cellPhoneNumber;

    @Size(max = 10)
    private String alternativeCellPhoneNumber;

    private Room roomNumber;

    private int numberOfTenantsInUnit;

    @PastOrPresent
    private LocalDate leaseStartDate;

    @FutureOrPresent
    private LocalDate leaseEndDate;

    @Size(max = 11)
    private String prepaidElectricityMeterNumber;

    private boolean depositPaid;

    @Column(precision = 10, scale = 2)
    private BigDecimal rental;

    private LocalDate paymentDate;

    private TenantBehaviour tenantBehaviour;

    @Enumerated(EnumType.STRING)
    private TenantStatus tenantStatus;
}