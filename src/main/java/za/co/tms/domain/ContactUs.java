package za.co.tms.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Convert;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Entity
public class ContactUs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Integer id;

    private String firstName;
    private String lastName;
    private String emailAddress;
    private String mobilePhoneNumber;

    @Enumerated(EnumType.STRING)
    private ReferenceAd whereDidYouHearAboutUs;

    private String message;
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'NEW'")
    private LeadStatus leadStatus;

    private LocalDateTime createdAt;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "accept_terms_and_conditions", columnDefinition = "CHAR(1)")
    private boolean acceptTermsAndConditions;

    @jakarta.persistence.Transient
    private String reCaptcha;

    @PrePersist
    public void prePersist() {
        if (this.leadStatus == null) {
            this.leadStatus = LeadStatus.NEW;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
