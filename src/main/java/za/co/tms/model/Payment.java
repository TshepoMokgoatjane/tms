package za.co.tms.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Tenant tenant;

    @Column(precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be zero or positive")
    @Schema(description = "Amount", example = "6000.00")
    private BigDecimal amount;

    @FutureOrPresent
    @Schema(description = "Payment date", example = "2026-11-01")
    private LocalDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Payment method", example = "CASH")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Payment status", example = "PAID")
    private PaymentStatus paymentStatus;
}