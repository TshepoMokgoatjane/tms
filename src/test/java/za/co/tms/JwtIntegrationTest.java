package za.co.tms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class JwtIntegrationTest {

    @SuppressWarnings("unused")
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateTokenAndAccessProtectedEndpoint() throws Exception {
        // Step 1: Add a user - Ensure the data below is unique (username, cellPhoneNumber, and email)
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());

        String userJson = String.format("""
                    {
                        "username": "tshepo_%s",
                        "password": "secure123",
                        "roles": "ROLE_USER",
                        "cellPhoneNumber": "012345678%s",
                        "email": "tshepo%s@tiktok.com",
                        "firstName": "Tshepo",
                        "lastName": "Mokgoatjane"
                    }
                """, uniqueSuffix, uniqueSuffix.substring(uniqueSuffix.length() - 2), uniqueSuffix);

        mockMvc.perform(post("/auth/addNewUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk());

        // Step 2: Generate token
        String loginJson = """
                    {
                        "username": "tshepo",
                        "password": "secure123"
                    }
                """;

        MvcResult result = mockMvc.perform(post("/auth/generateToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String token = result.getResponse().getContentAsString();

        // Step 3: Access protected endpoint
        mockMvc.perform(get("/auth/user/userProfile")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}