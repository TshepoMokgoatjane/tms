package za.co.tms.exception;

public class RoomAlreadyAssignedException extends RuntimeException {
    public RoomAlreadyAssignedException(String message) {
        super(message);
    }
}
