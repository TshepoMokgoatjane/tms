package za.co.tms.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "Next of kin contact information for a tenant")
@Entity
public class NextOfKin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    @Schema(description = "Unique identifier", example = "1")
    private Long id;

    @NotNull
    @Size(min = 1, max = 50)
    @Schema(description = "Next of kin's first name", example = "Thabo")
    private String firstName;

    @NotNull
    @Size(min = 1, max = 50)
    @Schema(description = "Next of kin's last name", example = "Mokgoatjane")
    private String lastName;

    @NotNull
    @Pattern(regexp = "^0\\d{9}$", message = "Phone number must be 10 digits starting with 0")
    @Size(max = 10)
    @Schema(description = "Next of kin's contact number", example = "0821234567")
    private String contactNumber;

    @Size(max = 50)
    @Schema(description = "Relationship to the tenant", example = "Spouse")
    private String relationship;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", unique = true)
    @Schema(hidden = true)
    private Tenant tenant;
}
