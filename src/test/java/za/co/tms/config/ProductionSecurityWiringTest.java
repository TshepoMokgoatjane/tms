package za.co.tms.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the production security wiring.
 *
 * <p>A {@code @TestConfiguration} that lived in src/main registered a second, permit-all
 * SecurityFilterChain in production. Depending on class-scan order of each build it could win,
 * which bypassed CorsConfig (live site blocked by CORS) and the URL access rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductionSecurityWiringTest {

    @Autowired
    private Map<String, SecurityFilterChain> securityFilterChains;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noTestOnlyConfigurationInProductionClasses() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        SimpleMetadataReaderFactory readers = new SimpleMetadataReaderFactory();
        List<String> offenders = new ArrayList<>();

        for (Resource resource : resolver.getResources("classpath*:za/co/tms/**/*.class")) {
            String url = resource.getURL().toString();
            if (url.contains("test-classes")) {
                continue; // test sources are allowed to hold test configuration
            }
            AnnotationMetadata metadata = readers.getMetadataReader(resource).getAnnotationMetadata();
            if (metadata.hasAnnotation(TestConfiguration.class.getName())
                    || metadata.hasMetaAnnotation(TestComponent.class.getName())) {
                offenders.add(metadata.getClassName());
            }
        }

        assertTrue(offenders.isEmpty(), "Test-only configuration found in src/main: " + offenders);
    }

    @Test
    void exactlyOneSecurityFilterChain() {
        assertEquals(1, securityFilterChains.size(), "Security chains: " + securityFilterChains.keySet());
    }

    @Test
    void liveSiteOriginPassesCorsPreflight() throws Exception {
        for (String origin : List.of("https://www.tltproperties.co.za", "https://tltproperties.co.za")) {
            mockMvc.perform(options("/authenticate")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void unknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/authenticate")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpointRejectsAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/payments/all"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403, "Expected 401/403 but was " + code);
                });
    }
}
