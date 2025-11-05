package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.tms.model.PaymentDay;
import za.co.tms.model.Room;
import za.co.tms.model.Tenant;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDTO {

    private Integer id;
    private String name;
    private String surname;
    private String title;
    private String email;
    private String cellPhoneNumber;
    private String alternativeCellPhoneNumber;

    private String roomCode;
    private String roomDescription;
    private String meterNumber;

    private int numberOfTenantsInUnit;
    private String leaseStartDate;
    private String leaseEndDate;
    private boolean depositPaid;
    private BigDecimal rental;

    private String paymentDayLabel;
    private String tenantBehaviour;
    private String tenantStatus;

    public TenantResponseDTO(Tenant tenant) {
        this.id = tenant.getId();
        this.name = tenant.getName();
        this.surname = tenant.getSurname();
        this.title = tenant.getTitle() != null ? tenant.getTitle().name() : null;
        this.email = tenant.getEmail();
        this.cellPhoneNumber = tenant.getCellPhoneNumber();
        this.alternativeCellPhoneNumber = tenant.getAlternativeCellPhoneNumber();

        Room room = tenant.getRoomNumber();
        this.roomCode = room != null ? room.name() : null;
        this.roomDescription = room != null ? room.getRoomDescription() : null;
        this.meterNumber = room != null ? room.getMeterNumber() : null;

        this.numberOfTenantsInUnit = tenant.getNumberOfTenantsInUnit();
        this.leaseStartDate = tenant.getLeaseStartDate() != null ? tenant.getLeaseStartDate().toString() : null;
        this.leaseEndDate = tenant.getLeaseEndDate() != null ? tenant.getLeaseEndDate().toString() : null;
        this.depositPaid = tenant.isDepositPaid();
        this.rental = tenant.getRental();

        PaymentDay paymentDay = tenant.getPaymentDay();
        this.paymentDayLabel = paymentDay != null ? paymentDay.getLabel() : null;

        this.tenantBehaviour = tenant.getTenantBehaviour() != null ? tenant.getTenantBehaviour().name() : null;
        this.tenantStatus = tenant.getTenantStatus() != null ? tenant.getTenantStatus().name() : null;
    }

}
