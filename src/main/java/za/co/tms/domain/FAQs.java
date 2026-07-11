package za.co.tms.domain;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class FAQs implements Serializable {

    @Serial
    private static final long serialVersionUID = -4830337221487671406L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Integer id;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FAQCategory category;

    private int displayOrder;

    @Column(nullable = false)
    private boolean active;
}
