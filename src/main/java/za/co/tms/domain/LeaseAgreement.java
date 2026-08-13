package za.co.tms.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "lease_agreement")
@Schema(description = "Lease agreement PDF document linked to a tenant")
public class LeaseAgreement extends AuditModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", unique = true, nullable = false)
    @Schema(description = "Tenant this lease agreement belongs to")
    private Tenant tenant;

    @Schema(description = "Original uploaded filename", example = "LeaseAgreement_Johnny.pdf")
    @Column(nullable = false)
    private String originalFilename;

    @Schema(description = "MIME content type", example = "application/pdf")
    @Column(nullable = false)
    private String contentType;

    @Schema(description = "File size in bytes", example = "524288")
    private Long fileSize;

    @Lob
    @JsonIgnore
    @Column(name = "pdf_data", columnDefinition = "LONGBLOB", nullable = false)
    @Schema(hidden = true)
    private byte[] pdfData;
}
