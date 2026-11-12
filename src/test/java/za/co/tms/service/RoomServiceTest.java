package za.co.tms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.tms.domain.Property;
import za.co.tms.domain.Room;
import za.co.tms.exception.RoomNotAdvertisableException;
import za.co.tms.repository.GalleryImageRepository;
import za.co.tms.repository.RoomRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private GalleryImageRepository galleryImageRepository;

    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private RoomService roomService;

    @Test
    void advertiseRoom_withNoProperty_throwsAndDoesNotSave() {
        Room room = new Room();
        room.setId(5L);
        room.setCode("A1");
        room.setProperty(null);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));

        RoomNotAdvertisableException ex = assertThrows(RoomNotAdvertisableException.class,
                () -> roomService.advertiseRoom(5L, null, null, null, null));
        assertEquals("This unit has no property assigned yet — assign it to a property first.",
                ex.getMessage());

        verify(roomRepository, never()).save(any());
    }

    @Test
    void advertiseRoom_whenPropertyHasNoLease_throwsAndDoesNotSave() {
        Property property = new Property();
        property.setId(10L);
        property.setStreetAddress("57 Caledon Drive");

        Room room = new Room();
        room.setId(5L);
        room.setCode("A1");
        room.setProperty(property);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(propertyService.findById(10L)).thenReturn(property);
        when(propertyService.hasLease(property)).thenReturn(false);

        RoomNotAdvertisableException ex = assertThrows(RoomNotAdvertisableException.class,
                () -> roomService.advertiseRoom(5L, null, null, null, null));
        assertEquals("Property 57 Caledon Drive has no lease agreement uploaded yet"
                + " — upload one before advertising.", ex.getMessage());

        verify(roomRepository, never()).save(any());
    }

    @Test
    void advertiseRoom_whenPropertyHasLease_advertisesAndPersists() {
        Property property = new Property();
        property.setId(10L);
        property.setStreetAddress("57 Caledon Drive");

        Room room = new Room();
        room.setId(5L);
        room.setCode("A1");
        room.setProperty(property);

        when(roomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(propertyService.findById(10L)).thenReturn(property);
        when(propertyService.hasLease(property)).thenReturn(true);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room result = roomService.advertiseRoom(5L, null, null, LocalDate.now(), null);

        assertTrue(result.isAdvertised(), "A room with a leased property must become advertised");
        verify(roomRepository).save(room);
    }
}
