package za.co.tms.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "property")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Schema(description = "Property/address owning a shared lease agreement PDF reused across its units")
public class Property extends AuditModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Schema(description = "Street address shown publicly", example = "57 Caledon Drive")
    @Column(unique = true, nullable = false)
    private String streetAddress;

    @Schema(description = "Internal ERF number", example = "1866")
    private String erfNumber;

    @Schema(description = "Original uploaded lease filename", example = "Lease_57_Caledon.pdf")
    private String leaseFilename;

    @Schema(description = "Lease MIME content type", example = "application/pdf")
    private String leaseContentType;

    @Schema(description = "Lease file size in bytes", example = "524288")
    private Long leaseFileSize;

    @Lob
    @JsonIgnore
    @Column(name = "lease_pdf_data", columnDefinition = "LONGBLOB")
    @Schema(hidden = true)
    private byte[] leasePdfData;

    /**
     * Metadata-only constructor used by list/projection queries so the heavy {@code leasePdfData}
     * blob is NEVER loaded for list views. {@code leasePdfData} is deliberately left null; it is
     * only populated when the lease is fetched by id for serving.
     */
    public Property(Long id, String streetAddress, String erfNumber, String leaseFilename,
                    String leaseContentType, Long leaseFileSize) {
        this.id = id;
        this.streetAddress = streetAddress;
        this.erfNumber = erfNumber;
        this.leaseFilename = leaseFilename;
        this.leaseContentType = leaseContentType;
        this.leaseFileSize = leaseFileSize;
    }
}
