package za.co.tms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @SuppressWarnings("unused")
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/auth/welcome"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointShouldBeForbiddenWithoutAuth() throws Exception {
        mockMvc.perform(get("/auth/user/userProfile"))
                .andExpect(status().isForbidden());
    }
}