package za.co.tms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class CorsConfig {

    /**
     * Production origins that must always be allowed, regardless of the configured
     * {@code cors.allowed-origins} / {@code CORS_ALLOWED_ORIGINS} value. Without these the live
     * site cannot reach the API at all (every request fails the CORS preflight with 403).
     */
    static final List<String> REQUIRED_ORIGINS = List.of(
            "https://www.tltproperties.co.za",
            "https://tltproperties.co.za",
            "https://tms-front-end-production.up.railway.app");

    @Value("${cors.allowed-origins:http://localhost:3000,https://tms-front-end-production.up.railway.app}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(resolveAllowedOrigins(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Merges the configured origins (comma-separated, whitespace and trailing slashes tolerated)
     * with {@link #REQUIRED_ORIGINS}, de-duplicated and in a stable order.
     */
    static List<String> resolveAllowedOrigins(String configured) {
        Set<String> origins = new LinkedHashSet<>();
        if (configured != null) {
            for (String origin : configured.split(",")) {
                String trimmed = origin.trim();
                while (trimmed.endsWith("/")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }
        origins.addAll(REQUIRED_ORIGINS);
        return new ArrayList<>(origins);
    }
}
