package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response returned when an ADMIN starts a read-only "View as Tenant" session.
 * Carries the impersonation token plus the tenant identity being viewed and the
 * admin who initiated it (for the frontend banner and session bookkeeping).
 */
@Data
@AllArgsConstructor
public class ImpersonationResponse {
    private String token;
    private String username;      // the tenant being viewed (token subject)
    private String role;          // always TENANT for impersonation
    private String displayName;   // tenant's full name for the banner
    private String impersonatedBy; // admin username
    private boolean readOnly;     // always true
}
