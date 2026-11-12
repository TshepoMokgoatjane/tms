package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;
import za.co.tms.repository.PropertyRepository;
import za.co.tms.repository.RoomRepository;

import java.io.IOException;
import java.util.List;

@Service
public class PropertyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public PropertyService(PropertyRepository propertyRepository, RoomRepository roomRepository) {
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
    }

    /**
     * Metadata-only list (never loads the lease PDF blob).
     */
    public List<Property> findAllMetadata() {
        LOGGER.info("Retrieving all property metadata");
        return propertyRepository.findAllMetadata();
    }

    /**
     * Fetch a single property (full entity) by id.
     */
    public Property findById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property with ID " + id + " not found."));
    }

    /**
     * Create a new property, rejecting a duplicate street address.
     */
    @Transactional
    public Property createProperty(Property property) {
        if (property.getStreetAddress() == null || property.getStreetAddress().isBlank()) {
            throw new IllegalArgumentException("Street address is required.");
        }
        if (propertyRepository.existsByStreetAddress(property.getStreetAddress())) {
            throw new IllegalArgumentException(
                    "A property with street address '" + property.getStreetAddress() + "' already exists.");
        }
        Property saved = propertyRepository.save(property);
        LOGGER.info("Property created: {} (ID {})", saved.getStreetAddress(), saved.getId());
        return saved;
    }

    /**
     * Update a property's street address and ERF number only. Never touches the lease blob.
     */
    @Transactional
    public Property updateProperty(Long id, Property updates) {
        Property property = findById(id);
        property.setStreetAddress(updates.getStreetAddress());
        property.setErfNumber(updates.getErfNumber());
        Property saved = propertyRepository.save(property);
        LOGGER.info("Property updated: {} (ID {})", saved.getStreetAddress(), saved.getId());
        return saved;
    }

    /**
     * Link a room to a property.
     */
    @Transactional
    public void assignRoom(Long propertyId, Long roomId) {
        Property property = findById(propertyId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room with ID " + roomId + " not found."));
        room.setProperty(property);
        roomRepository.save(room);
        LOGGER.info("Room ID {} assigned to property ID {}", roomId, propertyId);
    }

    /**
     * Unlink a room from its property.
     */
    @Transactional
    public void unassignRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room with ID " + roomId + " not found."));
        room.setProperty(null);
        roomRepository.save(room);
        LOGGER.info("Room ID {} unassigned from its property", roomId);
    }

    /**
     * Upload or replace (UPSERT) the shared lease PDF for a property.
     */
    @Transactional
    public Property uploadLease(Long propertyId, MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must be 10MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        // Validate property exists — upsert onto the existing property so re-upload overwrites
        Property property = findById(propertyId);

        property.setLeaseFilename(file.getOriginalFilename());
        property.setLeaseContentType(contentType);
        property.setLeaseFileSize(file.getSize());
        property.setLeasePdfData(file.getBytes());

        Property saved = propertyRepository.save(property);
        LOGGER.info("Lease uploaded for property ID {}: {} ({} bytes)",
                propertyId, file.getOriginalFilename(), file.getSize());

        return saved;
    }

    /**
     * Delete the lease PDF for a property, keeping the property itself.
     */
    @Transactional
    public void deleteLease(Long propertyId) {
        Property property = findById(propertyId);
        property.setLeaseFilename(null);
        property.setLeaseContentType(null);
        property.setLeaseFileSize(null);
        property.setLeasePdfData(null);
        propertyRepository.save(property);
        LOGGER.info("Lease deleted for property ID {}", propertyId);
    }

    /**
     * Retrieve the full property (including the lease blob) for download.
     */
    public Property getLeaseForDownload(Long propertyId) {
        return findById(propertyId);
    }

    /**
     * Whether the property has an uploaded lease.
     */
    public boolean hasLease(Property property) {
        return property != null && property.getLeaseFilename() != null;
    }
}
