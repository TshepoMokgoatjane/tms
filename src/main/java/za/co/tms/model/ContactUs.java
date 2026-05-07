package za.co.tms.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Convert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.type.YesNoConverter;

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

	@Convert(converter = YesNoConverter.class)
	@Column(name = "accept_terms_and_conditions", columnDefinition = "CHAR(1)")
	private boolean acceptTermsAndConditions;

	@jakarta.persistence.Transient
	private String reCaptcha;
}