package za.co.tms.exception;

public class InvalidLeasePeriodException extends RuntimeException {
    public InvalidLeasePeriodException(String message) {
        super(message);
    }
}