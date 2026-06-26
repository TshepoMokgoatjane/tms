package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.Room;
import za.co.tms.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

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
}
