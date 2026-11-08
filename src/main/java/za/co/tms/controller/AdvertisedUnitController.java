package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.Room;
import za.co.tms.dto.AdvertisedUnitDTO;
import za.co.tms.service.GalleryImageService;
import za.co.tms.service.RoomService;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Public, unauthenticated endpoints that power the "View Available Apartments" advert.
 * Only exposes units that are currently within their advertising window.
 */
@RestController
@RequestMapping("/api/advertised-units")
public class AdvertisedUnitController {

    private static final Logger log = LoggerFactory.getLogger(AdvertisedUnitController.class);

    private static final DateTimeFormatter AVAILABLE_FROM_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    /**
     * Bounded in-memory cache of resized image variants, keyed by {@code imageId + ":" + width}.
     * When it reaches {@link #RESIZE_CACHE_MAX} entries the whole map is cleared (a simple,
     * thread-safe coarse eviction). This is acceptable because variants are cheap to regenerate
     * and the cache exists only to guard heap growth, not to guarantee hit rates.
     */
    private static final Map<String, byte[]> RESIZE_CACHE = new ConcurrentHashMap<>();
    private static final int RESIZE_CACHE_MAX = 200;

    private final RoomService roomService;
    private final GalleryImageService galleryImageService;

    @Autowired
    public AdvertisedUnitController(RoomService roomService, GalleryImageService galleryImageService) {
        this.roomService = roomService;
        this.galleryImageService = galleryImageService;
    }

    @GetMapping
    @Operation(summary = "List advertised units", description = "Public list of units currently being advertised as available")
    public ResponseEntity<List<AdvertisedUnitDTO>> getAdvertisedUnits() {
        List<AdvertisedUnitDTO> units = roomService.findCurrentlyAdvertised().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(units);
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "Serve an advertised unit photo",
            description = "Returns the image binary for a unit photo. The optional `w` query param requests a "
                    + "downscaled variant (width in pixels, aspect ratio preserved); omitted, invalid or "
                    + "oversized values return the original bytes unchanged.")
    public ResponseEntity<byte[]> serveUnitImage(@PathVariable Long imageId,
                                                 @RequestParam(value = "w", required = false) Integer width) {
        GalleryImage image = galleryImageService.getGalleryImageById(imageId);
        if (image.getImageData() == null || image.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        String contentType = image.getContentType() != null ? image.getContentType() : "application/octet-stream";
        byte[] body = maybeResize(imageId, image.getImageData(), contentType, width);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(body);
    }

    /**
     * Returns a downscaled copy of {@code original} at the requested {@code width}, or the original
     * bytes unchanged when resizing is not applicable or safe. This method NEVER throws and NEVER
     * upscales: on any problem (unsupported type, undecodable bytes, requested width >= natural
     * width, or any decode/encode failure) it falls back to the original bytes so a resize attempt
     * can never turn into a 500.
     *
     * <p>Implemented on {@code javax.imageio} (JDK built-in) deliberately: the preferred
     * Thumbnailator dependency could not be resolved from the configured Maven repository in this
     * environment, so we fall back to the no-new-dependency option as the task allows.
     */
    private byte[] maybeResize(Long imageId, byte[] original, String contentType, Integer width) {
        if (width == null || width <= 0) {
            return original;
        }
        String outputFormat = resizableFormat(contentType);
        if (outputFormat == null) {
            // gif/webp/unknown/octet-stream: not something we can safely re-encode -> original bytes.
            return original;
        }
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(original));
            if (decoded == null) {
                return original;
            }
            int sourceWidth = decoded.getWidth();
            int sourceHeight = decoded.getHeight();
            if (width >= sourceWidth) {
                // Requested width is >= the natural width: never upscale.
                return original;
            }
            String cacheKey = imageId + ":" + width;
            byte[] cached = RESIZE_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            // Preserve aspect ratio; guard against a zero height for very wide thumbnails.
            int targetHeight = Math.max(1, Math.round((float) sourceHeight * width / sourceWidth));
            BufferedImage scaled = scale(decoded, width, targetHeight, outputFormat);
            byte[] resized = encode(scaled, outputFormat);

            if (RESIZE_CACHE.size() >= RESIZE_CACHE_MAX) {
                RESIZE_CACHE.clear();
            }
            RESIZE_CACHE.put(cacheKey, resized);
            return resized;
        } catch (Exception e) {
            log.warn("Image resize failed for imageId={} width={}, serving original bytes", imageId, width, e);
            return original;
        }
    }

    /**
     * High-quality down-scale into a new {@link BufferedImage}. JPEG has no alpha channel, so the
     * target is {@code TYPE_INT_RGB} for jpg and {@code TYPE_INT_ARGB} for png.
     */
    private BufferedImage scale(BufferedImage source, int targetWidth, int targetHeight, String outputFormat) {
        int type = "png".equals(outputFormat) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /**
     * Encodes the scaled image to bytes. JPEG is written at ~0.85 quality via an explicit
     * {@link ImageWriter}; PNG is lossless so it uses the default {@link ImageIO#write} path.
     */
    private byte[] encode(BufferedImage image, String outputFormat) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ("png".equals(outputFormat)) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            // No JPEG writer available: fall back to the default write path.
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.85f);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * Maps a content type to the ImageIO output format name we can safely re-encode to, or
     * {@code null} when the type is not a raster format we support resizing for.
     */
    private String resizableFormat(String contentType) {
        if (contentType == null) {
            return null;
        }
        String ct = contentType.toLowerCase(Locale.ENGLISH);
        if (ct.equals("image/jpeg") || ct.equals("image/jpg")) {
            return "jpg";
        }
        if (ct.equals("image/png")) {
            return "png";
        }
        return null;
    }

    private AdvertisedUnitDTO toDto(Room room) {
        List<Long> imageIds = roomService.getRoomImages(room.getId()).stream()
                .map(GalleryImage::getId)
                .collect(Collectors.toList());
        return new AdvertisedUnitDTO(
                room.getId(),
                room.getCode(),
                room.getDescription(),
                room.getRentalAmount(),
                room.getAdvertDescription(),
                room.getDocumentsRequired(),
                room.getAvailableFrom(),
                room.isOccupied(),
                buildAvailabilityLabel(room),
                imageIds);
    }

    /**
     * Human-friendly availability text for the advert:
     *  - empty unit            -> "Available now"
     *  - occupied, has a date  -> "Available from {d MMMM yyyy}"
     *  - occupied, no date yet  -> "Available soon"
     */
    private String buildAvailabilityLabel(Room room) {
        if (!room.isOccupied()) {
            return "Available now";
        }
        LocalDate from = room.getAvailableFrom();
        if (from != null) {
            return "Available from " + from.format(AVAILABLE_FROM_FORMAT);
        }
        return "Available soon";
    }
}
