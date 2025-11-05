package za.co.tms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.tms.config.NoSecurityConfig;
import za.co.tms.exception.TenantNotFoundException;
import za.co.tms.model.PaymentDay;
import za.co.tms.model.Room;
import za.co.tms.model.Tenant;
import za.co.tms.model.TenantBehaviour;
import za.co.tms.model.TenantStatus;
import za.co.tms.service.TenantService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(NoSecurityConfig.class)
public class TenantControllerTest {

    private static final String BASE_URL = "/tenants";
    private static final String FIND_ALL = BASE_URL + "/find/all";
    private static final String FIND_BY_NAME = BASE_URL + "/find/by-name/{name}";
    private static final String FIND_BY_ID = BASE_URL + "/find/by-id/{id}";
    private static final String CREATE = BASE_URL + "/create";
    private static final String UPDATE = BASE_URL + "/update/{id}";
    private static final String DELETE = BASE_URL + "/delete/{id}";
    private static final String AUTH_CHECK = BASE_URL + "/auth/check";

    private static final String TENANT_NAME = "Tshepo";
    private static final String TENANT_SURNAME = "Mokgoatjane";
    private static final String TENANT_EMAIL = "dummy@email.com";
    private static final String CELLPHONE_NUMBER = "0123456789";
    private static final String ALTERNATIVE_CELLPHONE_NUMBER = "0987654321";
    private static final int NUMBER_OF_TENANTS_IN_UNIT = 1;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void debugActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andDo(print());
    }

    @Test
    void shouldReturnAllTenants() throws Exception {

        // Get
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setTenantStatus(TenantStatus.ACTIVE);

        // When
        when(tenantService.findAllTenants()).thenReturn(Collections.singletonList(tenant));

        // Then
        mockMvc.perform(get(FIND_ALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(TENANT_NAME));
    }

    @Test
    void shouldCreateTenant() throws Exception {

        // Get
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setCellPhoneNumber(CELLPHONE_NUMBER);
        tenant.setAlternativeCellPhoneNumber(ALTERNATIVE_CELLPHONE_NUMBER);
        tenant.setRoomNumber(Room.A11);
        tenant.setNumberOfTenantsInUnit(NUMBER_OF_TENANTS_IN_UNIT);
        tenant.setPaymentDay(PaymentDay.DAY_1);
        tenant.setEmail(TENANT_EMAIL);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(5000));
        tenant.setTenantBehaviour(TenantBehaviour.GOOD);

        // When
        when(tenantService.addTenant(any(Tenant.class))).thenReturn(tenant);

        // Then
        mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TENANT_NAME));
    }

    @Test
    void shouldReturn400WhenNameIsMissing() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_SURNAME);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(5000));

        mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setEmail("invalid-email");
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(5000));

        mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldReturn400WhenLeaseEndDateIsBeforeStartDate() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().minusDays(1)); // Invalid
        tenant.setRental(BigDecimal.valueOf(5000));

        mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void shouldReturn400WhenRentalIsNegative() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);;
        tenant.setSurname(TENANT_SURNAME);
        tenant.setTenantStatus(TenantStatus.ACTIVE);
        tenant.setLeaseStartDate(LocalDate.now());
        tenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        tenant.setRental(BigDecimal.valueOf(-1000));    // Invalid rental

        mockMvc.perform(post(CREATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tenant)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.rental").value("Rental must be zero or positive"));
    }

    @Test
    void shouldReturnTenantsByName() throws Exception {

        // Get
        Tenant tenant = new Tenant();
        tenant.setName(TENANT_NAME);

        // When
        when(tenantService.findTenantByName("tshepo")).thenReturn(tenant);

        // Then
        mockMvc.perform(get("/tenants/find/by-name/tshepo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tshepo"));
    }

    @Test
    void shouldUpdateTenant() throws Exception {

        // Get
        int tenantId = 1;

        Tenant updateTenant = new Tenant();
        updateTenant.setName("Updated");
        updateTenant.setSurname("Tenant");
        updateTenant.setTenantStatus(TenantStatus.ACTIVE);
        updateTenant.setLeaseStartDate(LocalDate.now());
        updateTenant.setLeaseEndDate(LocalDate.now().plusDays(30));
        updateTenant.setRental(BigDecimal.valueOf(6000));

        // When
        when(tenantService.updateTenant(Mockito.eq(tenantId), any(Tenant.class)))
                .thenReturn(updateTenant);

        // Then
        mockMvc.perform(put(UPDATE, tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateTenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.surname").value("Tenant"));
    }

    @Test
    void shouldDeleteTenantById() throws Exception {
        int tenantId = 1;

        // No need to mock return value for void methods, just verify interaction
        Mockito.doNothing().when(tenantService).deleteTenantById(tenantId);

        mockMvc.perform(delete(DELETE, tenantId))
                .andExpect(status().isNoContent());

        Mockito.verify(tenantService).deleteTenantById(tenantId);
    }

    @Test
    void shouldReturnTenantById() throws Exception {
        int tenantId = 1;

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName(TENANT_NAME);
        tenant.setSurname(TENANT_SURNAME);
        tenant.setTenantStatus(TenantStatus.ACTIVE);

        when(tenantService.findTenantById(tenantId)).thenReturn(tenant);

        mockMvc.perform(get(FIND_BY_ID, tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.name").value("Tshepo"))
                .andExpect(jsonPath("$.surname").value("Mokgoatjane"));
    }

    @Test
    void shouldReturn404WhenTenantNotFoundById() throws Exception {
        int tenantId = 999;

        when(tenantService.findTenantById(tenantId))
                .thenThrow(new TenantNotFoundException("Tenant with ID " + tenantId + " not found"));

        mockMvc.perform(get(FIND_BY_ID, tenantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tenant with ID 999 not found"))
                .andExpect(jsonPath("$.errorCode").value("TENANT_NOT_FOUND"));
    }

    @Test
    void shouldReturn404WhenTenantNotFoundByName() throws Exception {
        String name = "nonexistent";

        when(tenantService.findTenantByName(name))
                .thenThrow(new TenantNotFoundException("Tenant with name '" + name + "' not found"));

        mockMvc.perform(get(FIND_BY_NAME, name))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tenant with name 'nonexistent' not found"))
                .andExpect(jsonPath("$.errorCode").value("TENANT_NOT_FOUND"));
    }

    @Test
    void shouldReturnSuccessFromBasicAuthCheck() throws Exception {
        mockMvc.perform(get(AUTH_CHECK))
                .andExpect(status().isOk())
                .andExpect(content().string("Success"));
    }
}