package za.co.tms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import za.co.tms.config.NoSecurityConfig;
import za.co.tms.exception.InvalidLeasePeriodException;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.jwt.JwtTokenService;
import za.co.tms.model.Tenant;
import za.co.tms.service.TenantService;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(NoSecurityConfig.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    public void testTenantNotFoundException() throws Exception {
        when(tenantService.findTenantById(999)).thenThrow(new TenantNotFoundException("Tenant with ID 999 was not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/tenants/find/by-id/999"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("TENANT_NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Tenant with ID 999 was not found"));
    }

    @Test
    public void testInvalidLeasePeriodException() throws Exception {

        Tenant tenant = new Tenant();
        tenant.setName("John");
        tenant.setSurname("Doe");
        tenant.setLeaseStartDate(LocalDate.of(2025, 10, 19));
        tenant.setLeaseEndDate(LocalDate.of(2025, 10, 20));

        when(tenantService.addTenant(any(Tenant.class)))
                .thenThrow(new InvalidLeasePeriodException("Lease end date must be after start date."));

        String tenantJson = """
                    {
                        "name": "John",
                        "surname": "Doe",
                        "leaseStartDate": "2025-10-19",
                        "leaseEndDate": "2025-10-20"
                    }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/tenants/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("INVALID_LEASE_PERIOD"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Lease end date must be after start date."));
    }

    @Test
    public void testGenericException() throws Exception {
        when(tenantService.findAllTenants()).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(MockMvcRequestBuilders.get("/tenants/find/all"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("An unexpected error occurred: Unexpected error"));
    }
}