package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.Room;
import za.co.tms.dto.AdvertisedUnitDTO;
import za.co.tms.service.GalleryImageService;
import za.co.tms.service.RoomService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public, unauthenticated endpoints that power the "View Available Apartments" advert.
 * Only exposes units that are currently within their advertising window.
 */
@RestController
@RequestMapping("/api/advertised-units")
public class AdvertisedUnitController {

    private final RoomService roomService;
    private final GalleryImageService galleryImageService;

    @Autowired
    public AdvertisedUnitController(RoomService roomService, GalleryImageService galleryImageService) {
        this.roomService = roomService;
        this.galleryImageService = galleryImageService;
    }

    @GetMapping
    @Operation(summary = "List advertised units", description = "Public list of units currently being advertised as available")
    public ResponseEntity<List<AdvertisedUnitDTO>> getAdvertisedUnits() {
        List<AdvertisedUnitDTO> units = roomService.findCurrentlyAdvertised().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(units);
    }

    @GetMapping("/{roomId}/images")
    @Operation(summary = "List advertised unit photo ids", description = "Ordered photo ids for a specific advertised unit")
    public ResponseEntity<List<Long>> getUnitImageIds(@PathVariable Long roomId) {
        List<Long> ids = roomService.getRoomImages(roomId).stream()
                .map(GalleryImage::getId)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "Serve an advertised unit photo", description = "Returns the image binary for a unit photo")
    public ResponseEntity<byte[]> serveUnitImage(@PathVariable Long imageId) {
        GalleryImage image = galleryImageService.getGalleryImageById(imageId);
        if (image.getImageData() == null || image.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String contentType = image.getContentType() != null ? image.getContentType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(image.getImageData());
    }

    private AdvertisedUnitDTO toDto(Room room) {
        List<Long> imageIds = roomService.getRoomImages(room.getId()).stream()
                .map(GalleryImage::getId)
                .collect(Collectors.toList());
        return new AdvertisedUnitDTO(
                room.getId(),
                room.getCode(),
                room.getDescription(),
                room.getRentalAmount(),
                room.getAdvertDescription(),
                room.getDocumentsRequired(),
                room.getAvailableFrom(),
                imageIds);
    }
}
