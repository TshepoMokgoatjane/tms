package za.co.tms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tms.config.NoSecurityConfig;
import za.co.tms.domain.Property;
import za.co.tms.service.GalleryImageService;
import za.co.tms.service.RoomService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the public lease-download endpoint GET /api/advertised-units/{roomId}/lease.
 * Kept separate from the plain-Mockito {@link AdvertisedUnitControllerTest} so the MockMvc wiring
 * does not mix with the pure image-resize unit tests.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(NoSecurityConfig.class)
public class AdvertisedUnitLeaseControllerTest {

    private static final String LEASE_URL = "/api/advertised-units/{roomId}/lease";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @MockBean
    private GalleryImageService galleryImageService;

    @Test
    void downloadUnitLease_whenLeaseExists_returnsPdfBytes() throws Exception {
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        Property property = new Property();
        property.setId(1L);
        property.setStreetAddress("57 Caledon Drive");
        property.setLeaseFilename("Lease_57_Caledon.pdf");
        property.setLeaseContentType("application/pdf");
        property.setLeaseFileSize((long) pdf.length);
        property.setLeasePdfData(pdf);

        when(roomService.getPropertyLeaseForRoom(eq(5L))).thenReturn(Optional.of(property));

        mockMvc.perform(get(LEASE_URL, 5L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"Lease_57_Caledon.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void downloadUnitLease_whenPropertyOrLeaseMissing_returnsNotFound() throws Exception {
        when(roomService.getPropertyLeaseForRoom(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get(LEASE_URL, 99L))
                .andExpect(status().isNotFound());
    }
}
