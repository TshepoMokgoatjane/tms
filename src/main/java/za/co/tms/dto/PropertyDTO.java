package za.co.tms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.tms.domain.Property;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Property metadata (without the actual lease PDF binary)")
public class PropertyDTO {

    @Schema(description = "Property ID", example = "1")
    private Long id;

    @Schema(description = "Street address shown publicly", example = "57 Caledon Drive")
    private String streetAddress;

    @Schema(description = "Internal ERF number", example = "1866")
    private String erfNumber;

    @Schema(description = "Original uploaded lease filename", example = "Lease_57_Caledon.pdf")
    private String leaseFilename;

    @Schema(description = "Lease file size in bytes", example = "524288")
    private Long leaseFileSize;

    @Schema(description = "Whether a lease agreement has been uploaded for this property")
    private boolean leaseAvailable;

    /**
     * Construct from entity. Never exposes {@code leasePdfData}.
     * {@code leaseAvailable} is true when a lease filename is present (a lease has been uploaded).
     */
    public PropertyDTO(Property property) {
        this.id = property.getId();
        this.streetAddress = property.getStreetAddress();
        this.erfNumber = property.getErfNumber();
        this.leaseFilename = property.getLeaseFilename();
        this.leaseFileSize = property.getLeaseFileSize();
        this.leaseAvailable = property.getLeaseFilename() != null;
    }
}
