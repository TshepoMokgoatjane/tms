package za.co.tms.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "house_rules_acknowledgement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "house_rules_id"}))
public class HouseRulesAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "house_rules_id", nullable = false)
    private HouseRules houseRules;

    @Column(nullable = false)
    private LocalDateTime acknowledgedAt;

    private String ipAddress;

    @PrePersist
    public void prePersist() {
        this.acknowledgedAt = LocalDateTime.now();
    }
}
