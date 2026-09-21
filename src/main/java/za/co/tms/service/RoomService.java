package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageCategory;
import za.co.tms.domain.GalleryImageStatus;
import za.co.tms.domain.Room;
import za.co.tms.exception.GalleryImageNotFoundException;
import za.co.tms.repository.GalleryImageRepository;
import za.co.tms.repository.RoomRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final RoomRepository roomRepository;
    private final GalleryImageRepository galleryImageRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, GalleryImageRepository galleryImageRepository) {
        this.roomRepository = roomRepository;
        this.galleryImageRepository = galleryImageRepository;
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

        return roomRepository.save(existingRoom);
    }

    public void deleteRoom(Long id) {
        LOGGER.info("Deleting room with ID {}", id);
        Room room = findById(id);
        // Remove any unit photos tied to this room so we don't orphan image rows.
        List<GalleryImage> images = galleryImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(id);
        if (!images.isEmpty()) {
            galleryImageRepository.deleteAll(images);
        }
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
     * Returns rooms that should be publicly visible right now: flagged as advertised, not occupied,
     * and within their advertising window (start already reached, end not yet passed).
     */
    public List<Room> findCurrentlyAdvertised() {
        LocalDate today = LocalDate.now();
        return roomRepository.findByAdvertisedTrue().stream()
                .filter(room -> !room.isOccupied())
                .filter(room -> room.getAdvertisingStartDate() == null || !room.getAdvertisingStartDate().isAfter(today))
                .filter(room -> room.getAdvertisingEndDate() == null || !room.getAdvertisingEndDate().isBefore(today))
                .collect(Collectors.toList());
    }

    // ===================== Per-unit photos =====================

    public List<GalleryImage> getRoomImages(Long roomId) {
        return galleryImageRepository.findByRoomIdOrderByDisplayOrderAscIdAsc(roomId);
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
