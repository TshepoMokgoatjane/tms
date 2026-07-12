package za.co.tms.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Testimonial implements Serializable {

 @Serial
 private static final long serialVersionUID = 1L;

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 @Column(updatable = false, nullable = false)
 private Integer id;

 @Column(nullable = false)
 private String tenantName;

 private String roomCode;

 @Column(columnDefinition = "TEXT", nullable = false)
 private String message;

 private int rating;

 private int displayOrder;

 @Column(nullable = false)
 private boolean active;

 private LocalDateTime createdAt;

 @PrePersist
 public void prePersist() {
 if (this.createdAt == null) {
 this.createdAt = LocalDateTime.now();
 }
 }
}
