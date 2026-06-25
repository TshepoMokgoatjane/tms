package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.Room;
import za.co.tms.repository.RoomRepository;

import java.util.List;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
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

        return roomRepository.save(existingRoom);
    }

    public void deleteRoom(Long id) {
        LOGGER.info("Deleting room with ID {}", id);
        Room room = findById(id);
        roomRepository.delete(room);
    }
}
