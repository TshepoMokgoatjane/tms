package za.co.tms.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class RecaptchaVerifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecaptchaVerifierService.class);

    private final WebClient webClient;

    public RecaptchaVerifierService(WebClient.Builder builder) {
        this. webClient = builder
                .baseUrl("https://www.google.com/recaptcha/api/siteverify")
                .build();
    }

    @Value("${recaptcha.secret}")
    private String secret;

    public boolean verify(String token, String remoteIp) {
        RecaptchaResponse recaptchaResponse = webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("secret", secret)
                        .with("response", token)
                        .with("remoteip", remoteIp == null ? "" : remoteIp))
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .block();

        if (recaptchaResponse == null) {
            // defensive
            LOGGER.warn("recaptcha response is null");
            return false;
        }

        if (Boolean.TRUE.equals(recaptchaResponse.success)) {
            return true;
        }

        LOGGER.warn("reCAPTCHA failed. error-codes={}", recaptchaResponse.errorCodes);

        return false;
    }
    static class RecaptchaResponse {
        @JsonProperty("success")
         public Boolean success;

        @JsonProperty("error-codes")
         public List<String> errorCodes;
    }
}