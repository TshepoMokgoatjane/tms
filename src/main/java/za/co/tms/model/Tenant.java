package za.co.tms.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
public class Tenant implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    private String name;
    private String email;
    private String roomNumber;
    private int numberOfTenantsInUnit;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private String prepaidElectricityMeter;
    private boolean paidDeposit;
    private BigDecimal rental;
    private LocalDate paymentDate;

}
