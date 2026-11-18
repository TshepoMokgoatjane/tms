package za.co.tms.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void productionDomainsAllowedEvenWhenConfiguredListOmitsThem() {
        // Mirrors the Railway CORS_ALLOWED_ORIGINS value that broke the live site.
        List<String> origins = CorsConfig.resolveAllowedOrigins(
                "http://localhost:3000,https://tms-front-end-production.up.railway.app");

        assertTrue(origins.contains("https://www.tltproperties.co.za"));
        assertTrue(origins.contains("https://tltproperties.co.za"));
        assertTrue(origins.contains("http://localhost:3000"));
    }

    @Test
    void configuredOriginsAreTrimmedAndDeduplicated() {
        List<String> origins = CorsConfig.resolveAllowedOrigins(
                " https://www.tltproperties.co.za/ , ,https://extra.example.com");

        assertEquals(1, origins.stream().filter("https://www.tltproperties.co.za"::equals).count());
        assertTrue(origins.contains("https://extra.example.com"));
        assertFalse(origins.contains(""));
    }

    @Test
    void nullConfigStillAllowsRequiredOrigins() {
        assertTrue(CorsConfig.resolveAllowedOrigins(null).containsAll(CorsConfig.REQUIRED_ORIGINS));
    }

    @Test
    void corsSourceAcceptsLiveSiteOrigin() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:3000");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/authenticate");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals("https://www.tltproperties.co.za", cors.checkOrigin("https://www.tltproperties.co.za"));
        assertNull(cors.checkOrigin("https://evil.example.com"));
    }
}
