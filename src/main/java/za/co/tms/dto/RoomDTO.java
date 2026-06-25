package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {

    private String roomNumber;
    private String code;
    private String description;
    private String prepaidElectricityMeterNumber;
    private BigDecimal rental;
}
