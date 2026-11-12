package za.co.tms.exception;

/**
 * Thrown when a unit cannot be advertised because its advertising prerequisites are not met:
 * it has no owning property assigned yet, or its property has no lease agreement uploaded.
 * The message explains exactly which prerequisite is missing so the admin can fix it.
 */
public class RoomNotAdvertisableException extends RuntimeException {
    public RoomNotAdvertisableException(String message) {
        super(message);
    }
}
