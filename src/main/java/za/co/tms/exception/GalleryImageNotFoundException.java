package za.co.tms.exception;

public class GalleryImageNotFoundException extends RuntimeException {
    public GalleryImageNotFoundException(Long id) {
        super("Gallery image with id " + id + " was not found");
    }
}
