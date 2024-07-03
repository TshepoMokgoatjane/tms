package za.co.tms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Ticket implements Serializable {
	/**
	 * Generated a serial version UID
	 */
	private static final long serialVersionUID = -8045581293415893683L;
	
	@Id
	@Column(updatable = false, nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	private Integer ticketNumber;	
	private String raisedBy;	
	private LocalDateTime dateRaised;	
	private String title;	
	private String description;	
	private String comments;	
	private Category category;	
	private Priority priority;	
	private Status status;
}
