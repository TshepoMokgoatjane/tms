package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.Room;
import za.co.tms.service.RoomService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/find/all")
    @Operation(summary = "Get all rooms", description = "Returns a list of all rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.findAll());
    }

    @GetMapping("/find/by/{id}")
    @Operation(summary = "Get room by ID", description = "Returns a room by its unique identifier")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.findById(id));
    }

    @GetMapping("/find/by-code/{code}")
    @Operation(summary = "Get room by code", description = "Returns a room by its unique code")
    public ResponseEntity<Room> getRoomByCode(@PathVariable String code) {
        return ResponseEntity.ok(roomService.findByCode(code));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Returns all unoccupied rooms")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        return ResponseEntity.ok(roomService.findAvailable());
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new room", description = "Adds a new room to the system")
    @ApiResponse(responseCode = "200", description = "Room created successfully")
    public ResponseEntity<Room> createRoom(@Valid @RequestBody Room room) {
        return ResponseEntity.ok(roomService.createRoom(room));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update a room", description = "Updates an existing room's details")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return ResponseEntity.ok(roomService.updateRoom(id, room));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a room", description = "Removes a room from the system")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Advertising (admin) =====================

    @PutMapping("/{id}/advertise")
    @Operation(summary = "Advertise a unit", description = "Marks a unit as available and starts advertising it publicly")
    public ResponseEntity<Room> advertiseRoom(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String advertDescription = body != null ? asString(body.get("advertDescription")) : null;
        String documentsRequired = body != null ? asString(body.get("documentsRequired")) : null;
        LocalDate availableFrom = body != null ? asDate(body.get("availableFrom")) : null;
        LocalDate advertisingEndDate = body != null ? asDate(body.get("advertisingEndDate")) : null;
        return ResponseEntity.ok(
                roomService.advertiseRoom(id, advertDescription, documentsRequired, availableFrom, advertisingEndDate));
    }

    @PutMapping("/{id}/stop-advertising")
    @Operation(summary = "Stop advertising a unit", description = "Removes a unit from the public advert list")
    public ResponseEntity<Room> stopAdvertisingRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.stopAdvertisingRoom(id));
    }

    // ===================== Per-unit photos (admin) =====================

    @GetMapping("/{id}/images")
    @Operation(summary = "List unit photos", description = "Returns the photos attached to a specific unit")
    public ResponseEntity<List<GalleryImage>> getRoomImages(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomImages(id));
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Upload a unit photo", description = "Attaches a photo to a specific unit (stored with category UNIT)")
    public ResponseEntity<GalleryImage> uploadRoomImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            GalleryImage saved = roomService.addRoomImage(
                    id, file.getContentType(), file.getOriginalFilename(), file.getSize(), file.getBytes(), title);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rejected unit photo upload for room {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            LOGGER.error("Failed to read uploaded unit photo for room {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/images/{imageId}")
    @Operation(summary = "Delete a unit photo", description = "Removes a photo from a unit")
    public ResponseEntity<Void> deleteRoomImage(@PathVariable Long imageId) {
        roomService.deleteRoomImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // ----- helpers for the flexible advertise request body -----

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static LocalDate asDate(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return LocalDate.parse(value.toString());
    }
}
