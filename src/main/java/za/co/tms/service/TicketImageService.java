package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.TicketImage;
import za.co.tms.repository.TicketImageRepository;
import za.co.tms.repository.TicketRepository;

import java.util.List;
import java.util.Set;

/**
 * Manages photos attached to maintenance tickets. Photos are optional, stored as bytes in the
 * database, and capped per ticket to bound storage.
 */
@Service
public class TicketImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketImageService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /** Maximum photos a single ticket may carry. */
    public static final int MAX_IMAGES_PER_TICKET = 5;
    /** Maximum size per photo (10MB). */
    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final TicketImageRepository ticketImageRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public TicketImageService(TicketImageRepository ticketImageRepository, TicketRepository ticketRepository) {
        this.ticketImageRepository = ticketImageRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<TicketImage> getTicketImages(Long ticketId) {
        return ticketImageRepository.findByTicketIdOrderByUploadedAtAscIdAsc(ticketId);
    }

    public TicketImage getImage(Long imageId) {
        return ticketImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Ticket image " + imageId + " not found"));
    }

    /**
     * Attach a photo to a ticket after validating the ticket exists, the file type/size are
     * allowed, and the per-ticket cap has not been reached.
     */
    public TicketImage addTicketImage(Long ticketId, String contentType, String originalFilename,
                                      long fileSize, byte[] data) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new IllegalArgumentException("Ticket " + ticketId + " not found");
        }
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported image type: " + contentType);
        }
        if (fileSize > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image exceeds the 10MB limit");
        }
        if (ticketImageRepository.countByTicketId(ticketId) >= MAX_IMAGES_PER_TICKET) {
            throw new IllegalStateException("A ticket may have at most " + MAX_IMAGES_PER_TICKET + " photos");
        }

        TicketImage image = new TicketImage();
        image.setTicketId(ticketId);
        image.setOriginalFilename(originalFilename);
        image.setContentType(contentType);
        image.setFileSize(fileSize);
        image.setImageData(data);

        TicketImage saved = ticketImageRepository.save(image);
        LOGGER.info("Attached photo {} to ticket {}", saved.getId(), ticketId);
        return saved;
    }

    public void deleteImage(Long imageId) {
        ticketImageRepository.deleteById(imageId);
        LOGGER.info("Deleted ticket photo {}", imageId);
    }
}
