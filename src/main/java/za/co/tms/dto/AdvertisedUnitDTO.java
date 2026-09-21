package za.co.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Public-facing view of an advertised unit. Deliberately excludes internal fields
 * (e.g. prepaid meter number) and exposes only what a prospective tenant should see.
 * Image IDs are ordered and can be turned into image URLs by the frontend so each
 * unit's photo carousel only shows that unit's photos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisedUnitDTO {

    private Long id;
    private String code;
    private String type;             // Room.description holds the unit type (e.g. "Bachelor Flat")
    private BigDecimal rentalAmount;
    private String description;      // marketing copy
    private String documentsRequired;
    private LocalDate availableFrom;
    private List<Long> imageIds;     // ordered photo ids for this specific unit
}
