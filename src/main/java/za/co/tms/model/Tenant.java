package za.co.tms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
	private static final long serialVersionUID = 7011620956908743611L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Integer id;

    private String name;
    private String surname;
    private String title;
    private String email;
    private String cellPhoneNumber;
    private String alternativeCellPhoneNumber;
    private String roomNumber;
    private int numberOfTenantsInUnit;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private String prepaidElectricityMeter;
    private boolean depositPaid;
    private double rental;
    private LocalDate paymentDate;
    private TenantBehaviour tenantBehaviour;   
}