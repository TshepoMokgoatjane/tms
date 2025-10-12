package za.co.tms.validator;

import org.springframework.stereotype.Component;
import za.co.tms.exception.InvalidLeasePeriodException;
import za.co.tms.model.Tenant;

@Component
public class TenantValidator {

    public void validateLeaseDates(Tenant tenant) {
        if (tenant.getLeaseStartDate() != null && tenant.getLeaseEndDate() != null
        &&
                tenant.getLeaseEndDate().isBefore(tenant.getLeaseStartDate())) {
            throw new InvalidLeasePeriodException("Lease end date must be after start date.");
        }
    }
}