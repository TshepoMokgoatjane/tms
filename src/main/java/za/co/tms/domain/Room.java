package za.co.tms.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "room")
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String code;

	private String description;

	private BigDecimal rentalAmount;

	private String prepaidElectricityMeterNumber;

	private boolean occupied;

	// ===== Advertising fields =====

	/** Whether this unit is currently being advertised as available. */
	private boolean advertised;

	/** Date advertising started (set manually by admin or automatically by the scheduler). */
	private LocalDate advertisingStartDate;

	/** Optional date advertising should stop. Null means "until occupied / stopped manually". */
	private LocalDate advertisingEndDate;

	/** The date the unit becomes / became available (typically the outgoing tenant's lease end date). */
	private LocalDate availableFrom;

	/** Rich marketing description shown on the public advert. */
	@Column(columnDefinition = "TEXT")
	private String advertDescription;

	/** Documents a prospective tenant must supply (one per line). */
	@Column(columnDefinition = "TEXT")
	private String documentsRequired;

	private LocalDate createdAt;
	private LocalDateTime updatedAt;

	// The toString matches what the frontend displays
	public String getRoomNumber() {
		return code + " - " + description;
	}

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDate.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}