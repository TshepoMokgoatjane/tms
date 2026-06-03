package za.co.tms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import za.co.tms.repository.AppUserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class JwtIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }

    @Test
    void shouldGenerateTokenAndAccessProtectedEndpoint() throws Exception {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());

        // Step 1: Register a user
        String userJson = String.format("""
                    {
                        "username": "tshepo_%s",
                        "password": "secure123",
                        "role": "USER",
                        "cellPhoneNumber": "012345678%s",
                        "email": "tshepo%s@tiktok.com",
                        "firstName": "Tshepo",
                        "lastName": "Mokgoatjane"
                    }
                """, uniqueSuffix, uniqueSuffix.substring(uniqueSuffix.length() - 2), uniqueSuffix);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isCreated());

        // Step 2: Generate token
        String loginJson = String.format("""
                    {
                        "username": "tshepo_%s",
                        "password": "secure123"
                    }
                """, uniqueSuffix);

        MvcResult result = mockMvc.perform(post("/auth/generateToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        String token = json.get("token").asText();

        Assertions.assertNotNull(token);
        Assertions.assertTrue(token.startsWith("ey"));

        // Step 3: Access protected endpoint
        mockMvc.perform(get("/auth/user/profile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
