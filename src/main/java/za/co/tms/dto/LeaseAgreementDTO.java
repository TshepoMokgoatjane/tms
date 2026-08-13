package za.co.tms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.tms.domain.LeaseAgreement;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Lease agreement metadata (without the actual PDF binary)")
public class LeaseAgreementDTO {

    @Schema(description = "Lease agreement ID", example = "1")
    private Long id;

    @Schema(description = "Tenant ID", example = "5")
    private Integer tenantId;

    @Schema(description = "Tenant full name", example = "Johnny Mokgoatjane")
    private String tenantName;

    @Schema(description = "Room code", example = "A1")
    private String roomCode;

    @Schema(description = "Original uploaded filename", example = "LeaseAgreement_Johnny.pdf")
    private String fileName;

    @Schema(description = "File size in bytes", example = "524288")
    private Long fileSize;

    @Schema(description = "Upload timestamp")
    private Date uploadedAt;

    @Schema(description = "Whether a lease agreement exists for this tenant")
    private boolean exists;

    /**
     * Construct from entity (for list/check responses).
     */
    public LeaseAgreementDTO(LeaseAgreement lease) {
        this.id = lease.getId();
        this.tenantId = lease.getTenant() != null ? lease.getTenant().getId() : null;
        this.tenantName = lease.getTenant() != null
                ? lease.getTenant().getName() + " " + lease.getTenant().getSurname()
                : null;
        this.roomCode = (lease.getTenant() != null && lease.getTenant().getRoom() != null)
                ? lease.getTenant().getRoom().getCode()
                : null;
        this.fileName = lease.getOriginalFilename();
        this.fileSize = lease.getFileSize();
        this.uploadedAt = lease.getCreatedAt();
        this.exists = true;
    }

    /**
     * Static factory for "not found" / check response when no lease exists.
     */
    public static LeaseAgreementDTO notFound(Integer tenantId) {
        LeaseAgreementDTO dto = new LeaseAgreementDTO();
        dto.setTenantId(tenantId);
        dto.setExists(false);
        return dto;
    }
}
