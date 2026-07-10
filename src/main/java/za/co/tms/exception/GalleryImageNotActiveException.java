package za.co.tms.exception;

public class GalleryImageNotActiveException extends RuntimeException {
    public GalleryImageNotActiveException(Long id) {
        super("Gallery image not active for id " + id);
    }
}
