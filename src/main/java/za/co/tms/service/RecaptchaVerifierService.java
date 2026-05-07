package za.co.tms.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RecaptcharVerifierService {
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.google.com/recaptcha/api/siteverify")
            .build();

    @Value("${recaptcha.secret")
    private String secret;

    public Mono<Boolean> verify(String token, String remoteIp) {
        return webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("secret", secret)
                        .with("response", token)
                        .with("remoteip", remoteIp == null ? "" : remoteIp))
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .map(RecaptchaResponse::isSuccess)
                .onErrorReturn(false);
    }
    static class RecaptchaResponse {
        @JsonProperty("success")
         public Boolean success;

        @JsonProperty("error-codes")
         public List<String> errorCodes;

        public boolean isSuccess() {
            return Boolean.TRUE.equals(success);
        }
    }
}
