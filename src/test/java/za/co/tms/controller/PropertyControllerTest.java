package za.co.tms.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tms.config.NoSecurityConfig;
import za.co.tms.domain.Property;
import za.co.tms.service.PropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(NoSecurityConfig.class)
public class PropertyControllerTest {

    private static final long PROPERTY_ID = 1L;
    private static final String UPLOAD_URL = "/properties/{propertyId}/lease";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;

    private Property savedProperty(String filename) {
        Property property = new Property();
        property.setId(PROPERTY_ID);
        property.setStreetAddress("57 Caledon Drive");
        property.setErfNumber("1866");
        property.setLeaseFilename(filename);
        property.setLeaseContentType("application/pdf");
        property.setLeaseFileSize(1024L);
        return property;
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void shouldReturn400WhenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        when(propertyService.uploadLease(eq(PROPERTY_ID), any()))
                .thenThrow(new IllegalArgumentException("File is empty."));

        mockMvc.perform(multipart(UPLOAD_URL, PROPERTY_ID).file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void shouldReturn400WhenFileIsOversize() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1, 2, 3});

        when(propertyService.uploadLease(eq(PROPERTY_ID), any()))
                .thenThrow(new IllegalArgumentException("File must be 10MB or smaller."));

        mockMvc.perform(multipart(UPLOAD_URL, PROPERTY_ID).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("File must be 10MB or smaller."));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void shouldReturn400WhenContentTypeIsNotPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

        when(propertyService.uploadLease(eq(PROPERTY_ID), any()))
                .thenThrow(new IllegalArgumentException("Only PDF files are allowed."));

        mockMvc.perform(multipart(UPLOAD_URL, PROPERTY_ID).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("Only PDF files are allowed."));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void shouldReturn200WhenValidPdfUploaded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lease.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.4".getBytes());

        when(propertyService.uploadLease(eq(PROPERTY_ID), any()))
                .thenReturn(savedProperty("lease.pdf"));

        mockMvc.perform(multipart(UPLOAD_URL, PROPERTY_ID).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) PROPERTY_ID))
                .andExpect(jsonPath("$.leaseFilename").value("lease.pdf"))
                .andExpect(jsonPath("$.leaseAvailable").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void shouldReturn200WhenValidPdfReUploadedUpsert() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lease-v2.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.7".getBytes());

        when(propertyService.uploadLease(eq(PROPERTY_ID), any()))
                .thenReturn(savedProperty("lease-v2.pdf"));

        mockMvc.perform(multipart(UPLOAD_URL, PROPERTY_ID).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaseFilename").value("lease-v2.pdf"))
                .andExpect(jsonPath("$.leaseAvailable").value(true));
    }
}

