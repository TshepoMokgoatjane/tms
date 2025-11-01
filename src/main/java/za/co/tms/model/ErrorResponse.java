package za.co.tms.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ErrorResponse {

    private String errorCode;
    private String message;
    private Map<String, String> errors;

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorResponse(String errorCode, String message, Map<String, String> errors) {
        this.errorCode = errorCode;
        this.message = message;
        this.errors = errors;
    }
}