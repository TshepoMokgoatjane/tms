package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageCategory;
import za.co.tms.domain.GalleryImageStatus;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;
import za.co.tms.exception.GalleryImageNotFoundException;
import za.co.tms.exception.RoomNotAdvertisableException;
import za.co.tms.repository.GalleryImageRepository;
import za.co.tms.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final RoomRepository roomRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final PropertyService propertyService;

    @Autowired
    public RoomService(RoomRepository roomRepository, GalleryImageRepository galleryImageRepository,
                       PropertyService propertyService) {
        this.roomRepository = roomRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.propertyService = propertyService;
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public Room findById(Long id) {
        LOGGER.info("Find room with ID {}", id);
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room with ID " + id + " not found"));
    }

    public Room findByCode(String code) {
        LOGGER.info("Find room with code {}", code);
        return roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Room with code '" + code + "' not found"));
    }

    public List<Room> findAvailable() {
        return roomRepository.findByOccupiedFalse();
    }

    public Room createRoom(Room room) {
        LOGGER.info("Creating new room with code {}", room.getCode());
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room updatedRoom) {
        LOGGER.info("Updating room with ID {}", id);

        Room existingRoom = findById(id);
        existingRoom.setCode(updatedRoom.getCode());
        existingRoom.setDescription(updatedRoom.getDescription());
        existingRoom.setRentalAmount(updatedRoom.getRentalAmount());
        existingRoom.setPrepaidElectricityMeterNumber(updatedRoom.getPrepaidElectricityMeterNumber());
        existingRoom.setOccupied(updatedRoom.isOccupied());

        // Advertising content (marketing copy + required documents) is editable from the room form.
        existingRoom.setAdvertDescription(updatedRoom.getAdvertDescription());
        existingRoom.setDocumentsRequired(updatedRoom.getDocumentsRequired());

        // Persist the owning-property link when the form supplies one (dropdown in FEAT-004). A
        // property with a null id means "no property selected" and detaches the room; a property
        // with an id is resolved to the managed entity so the room.property FK actually saves.
        existingRoom.setProperty(resolveProperty(updatedRoom.getProperty()));

        return roomRepository.save(existingRoom);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteRoom(Long id) {
        LOGGER.info("Deleting room with ID {}", id);
        Room room = findById(id);
        // Remove any unit photos tied to this room (bulk delete, no blobs loaded into memory).
        galleryImageRepository.deleteByRoomId(id);
        roomRepository.delete(room);
    }

    // ===================== Advertising =====================

    /**
     * Start advertising a unit. Optionally accepts fresh marketing copy / documents and an
     * "available from" date. Advertising runs until it is stopped or the room is re-occupied.
     */
    public Room advertiseRoom(Long id, String advertDescription, String documentsRequired,
                              LocalDate availableFrom, LocalDate advertisingEndDate) {
        Room room = findById(id);

        // A unit can only be advertised once it belongs to a property AND that property has a lease
        // uploaded — otherwise a prospective tenant could not download the lease for it.
        Property property = room.getProperty();
        if (property == null) {
            throw new RoomNotAdvertisableException(
                    "This unit has no property assigned yet — assign it to a property first.");
        }
        Property resolved = propertyService.findById(property.getId());
        if (!propertyService.hasLease(resolved)) {
            throw new RoomNotAdvertisableException(
                    "Property " + resolved.getStreetAddress()
                            + " has no lease agreement uploaded yet — upload one before advertising.");
        }

        room.setAdvertised(true);
        room.setAdvertisingStartDate(LocalDate.now());
        room.setAdvertisingEndDate(advertisingEndDate);
        if (availableFrom != null) {
            room.setAvailableFrom(availableFrom);
        }
        if (advertDescription != null) {
            room.setAdvertDescription(advertDescription);
        }
        if (documentsRequired != null) {
            room.setDocumentsRequired(documentsRequired);
        }
        LOGGER.info("Advertising started for room {} (code {})", id, room.getCode());
        return roomRepository.save(room);
    }

    /** Stop advertising a unit (manual admin action). */
    public Room stopAdvertisingRoom(Long id) {
        Room room = findById(id);
        room.setAdvertised(false);
        room.setAdvertisingEndDate(LocalDate.now());
        LOGGER.info("Advertising stopped for room {} (code {})", id, room.getCode());
        return roomRepository.save(room);
    }

    /**
     * Returns rooms that should be publicly visible right now: flagged as advertised and within
     * their advertising window (start already reached, end not yet passed).
     *
     * <p>Occupancy is deliberately NOT a filter here. A unit can be advertised while the outgoing
     * tenant is still in place (e.g. advertising a month before the lease ends, or a mid-month
     * departure) so we can line up the next tenant and avoid a vacancy gap. The advert instead
     * communicates WHEN the unit is available via {@code availableFrom} / the occupancy flag, which
     * the frontend turns into an "Available now" / "Available from {date}" / "Available soon" label.
     */
    public List<Room> findCurrentlyAdvertised() {
        LocalDate today = LocalDate.now();
        return roomRepository.findByAdvertisedTrue().stream()
                .filter(room -> room.getAdvertisingStartDate() == null || !room.getAdvertisingStartDate().isAfter(today))
                .filter(room -> room.getAdvertisingEndDate() == null || !room.getAdvertisingEndDate().isBefore(today))
                .collect(Collectors.toList());
    }

    // ===================== Property / lease resolution (advert DTO helpers) =====================

    /**
     * Resolves the full Property-with-lease owning the given room, or empty when the room, its
     * property, or the property's lease bytes are missing. Used by the public lease-download
     * endpoint to stream the PDF only when it genuinely exists.
     */
    public Optional<Property> getPropertyLeaseForRoom(Long roomId) {
        Room room = findById(roomId);
        Property property = room.getProperty();
        if (property == null) {
            return Optional.empty();
        }
        Property resolved = propertyService.findById(property.getId());
        if (resolved.getLeasePdfData() == null || resolved.getLeasePdfData().length == 0) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    /**
     * Whether the room's owning property has an uploaded lease. Resolves the property fresh so a
     * lazily-loaded association does not hide the lease metadata.
     */
    public boolean isLeaseAvailable(Room room) {
        if (room == null || room.getProperty() == null) {
            return false;
        }
        return propertyService.hasLease(propertyService.findById(room.getProperty().getId()));
    }

    /**
     * Public street address of the room's owning property, or null when the room has no property.
     * ERF stays internal and is deliberately never exposed here.
     */
    public String resolveStreetAddress(Room room) {
        if (room == null || room.getProperty() == null) {
            return null;
        }
        return propertyService.findById(room.getProperty().getId()).getStreetAddress();
    }

    /**
     * Resolves an incoming property reference (from a room form payload) to a managed entity, or
     * null when none/blank was supplied so the room is detached. A reference with a null id means
     * "no property selected".
     */
    private Property resolveProperty(Property incoming) {
        if (incoming == null || incoming.getId() == null) {
            return null;
        }
        return propertyService.findById(incoming.getId());
    }

    // ===================== Per-unit photos =====================

    public List<GalleryImage> getRoomImages(Long roomId) {
        // Metadata only — callers use ids/metadata; the blob is served separately by id.
        return galleryImageRepository.findByRoomIdMetadata(roomId);
    }

    /**
     * Store a photo bound to a specific room. Photos are tagged with category UNIT and the roomId
     * so a unit's images never mix with the general gallery or with another unit.
     */
    public GalleryImage addRoomImage(Long roomId, String contentType, String originalFilename,
                                     long fileSize, byte[] data, String title) {
        // Ensure the room exists before attaching an image to it.
        findById(roomId);

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type: " + contentType);
        }

        GalleryImage image = new GalleryImage();
        image.setTitle(title);
        image.setOriginalFilename(originalFilename);
        image.setContentType(contentType);
        image.setFileSize(fileSize);
        image.setImageData(data);
        image.setCategory(GalleryImageCategory.UNIT);
        image.setStatus(GalleryImageStatus.ACTIVE);
        image.setDisplayOrder(getRoomImages(roomId).size());
        image.setRoomId(roomId);

        GalleryImage saved = galleryImageRepository.save(image);
        LOGGER.info("Added unit photo {} to room {}", saved.getId(), roomId);
        return saved;
    }

    public void deleteRoomImage(Long imageId) {
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new GalleryImageNotFoundException(imageId));
        galleryImageRepository.delete(image);
        LOGGER.info("Deleted unit photo {}", imageId);
    }
}
