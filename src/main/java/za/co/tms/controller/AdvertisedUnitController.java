package za.co.tms.controller;

// Redeploy trigger: re-run the Railway build for the EXIF orientation fix.
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
import za.co.tms.domain.Property;
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
import java.awt.geom.AffineTransform;
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

    @GetMapping("/{roomId}/lease")
    @Operation(summary = "Download the lease for an advertised unit",
            description = "Public download of the shared lease agreement PDF for the unit's owning property. "
                    + "Returns 404 when the unit has no property or the property has no uploaded lease.")
    public ResponseEntity<byte[]> downloadUnitLease(@PathVariable Long roomId) {
        return roomService.getPropertyLeaseForRoom(roomId)
                .map(property -> {
                    String filename = property.getLeaseFilename() != null
                            ? property.getLeaseFilename()
                            : "lease.pdf";
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .body(property.getLeasePdfData());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "Serve an advertised unit photo",
            description = "Returns the image binary for a unit photo. The optional `w` query param requests a "
                    + "downscaled variant (width in pixels, aspect ratio preserved); omitted, invalid or "
                    + "oversized values return the original bytes unchanged. Downscaled JPEG variants are "
                    + "corrected for EXIF orientation so phone photos display upright.")
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
     *
     * <p><strong>EXIF orientation:</strong> {@link ImageIO#read} decodes JPEG pixels in their raw
     * sensor order and does NOT auto-apply the EXIF Orientation tag, and re-encoding drops the EXIF
     * metadata a browser would otherwise honour. Phone photos that relied on the Orientation tag
     * would therefore come out rotated/sideways in the downscaled variant. To fix this we read the
     * orientation ourselves ({@link #readJpegOrientation}) and re-orient the decoded image
     * ({@link #applyOrientation}) BEFORE downscaling, on the JPEG resize path only. Already-upright
     * photos (orientation 1 / absent / unreadable) are left byte-for-byte as they are today.
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
            // Re-orient JPEGs per their EXIF Orientation tag before downscaling (PNG carries no
            // EXIF orientation). Orientation 1/absent/unreadable returns the same image unchanged,
            // so already-upright photos are not affected.
            BufferedImage oriented = decoded;
            if ("jpg".equals(outputFormat)) {
                int orientation = readJpegOrientation(original);
                oriented = applyOrientation(decoded, orientation, outputFormat);
            }
            // Dimensions AFTER orientation (display dimensions): for EXIF 5-8 width/height are
            // swapped, so the upscale guard and the target-width math must use these, not the raw
            // decoded dimensions, for the requested width and aspect ratio to stay correct.
            int sourceWidth = oriented.getWidth();
            int sourceHeight = oriented.getHeight();
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
            BufferedImage scaled = scale(oriented, width, targetHeight, outputFormat);
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

    /**
     * Reads the EXIF Orientation tag (TIFF tag {@code 0x0112}, legal values 1..8) straight out of a
     * JPEG's raw bytes using only JDK primitives, because {@link ImageIO#read} does not surface it.
     *
     * <p>We scan the APP1 marker segments for the {@code "Exif\0\0"} header, parse the TIFF header
     * (supporting both {@code "II"} little-endian and {@code "MM"} big-endian byte orders), walk the
     * IFD0 directory entries and return the Orientation SHORT when present and in range.
     *
     * <p>This method NEVER throws: any missing segment, out-of-range value, truncated or malformed
     * data yields {@code 1} (normal / no transform) so the caller simply falls back to today's
     * behaviour and a resize attempt can never fail because of EXIF parsing.
     *
     * @param jpeg the original JPEG bytes
     * @return the orientation value 1..8, or {@code 1} when absent/unreadable
     */
    int readJpegOrientation(byte[] jpeg) {
        try {
            if (jpeg == null || jpeg.length < 4) {
                return 1;
            }
            // JPEG must start with SOI (0xFFD8).
            if ((jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
                return 1;
            }
            int offset = 2;
            while (offset + 4 <= jpeg.length) {
                if ((jpeg[offset] & 0xFF) != 0xFF) {
                    // Not a marker where one is expected: give up safely.
                    return 1;
                }
                int marker = jpeg[offset + 1] & 0xFF;
                // Standalone markers without a length payload: SOI, EOI, TEMy, RSTn.
                if (marker == 0xD8 || marker == 0xD9) {
                    return 1;
                }
                // Start of scan: EXIF (if any) has already appeared before image data.
                if (marker == 0xDA) {
                    return 1;
                }
                int segLength = ((jpeg[offset + 2] & 0xFF) << 8) | (jpeg[offset + 3] & 0xFF);
                if (segLength < 2 || offset + 2 + segLength > jpeg.length) {
                    return 1;
                }
                int segStart = offset + 4;
                int segDataLength = segLength - 2;
                // APP1 segment carrying EXIF.
                if (marker == 0xE1 && segDataLength >= 6
                        && jpeg[segStart] == 'E' && jpeg[segStart + 1] == 'x'
                        && jpeg[segStart + 2] == 'i' && jpeg[segStart + 3] == 'f'
                        && jpeg[segStart + 4] == 0 && jpeg[segStart + 5] == 0) {
                    return parseExifOrientation(jpeg, segStart + 6, segStart + segDataLength);
                }
                offset += 2 + segLength;
            }
            return 1;
        } catch (Exception e) {
            // Defensive: never let EXIF parsing break the resize path.
            return 1;
        }
    }

    /**
     * Parses the TIFF header + IFD0 beginning at {@code tiffStart} (inclusive) up to {@code limit}
     * (exclusive) and returns the Orientation tag value, or {@code 1} on any problem.
     */
    private int parseExifOrientation(byte[] data, int tiffStart, int limit) {
        if (tiffStart + 8 > limit) {
            return 1;
        }
        boolean bigEndian;
        int b0 = data[tiffStart] & 0xFF;
        int b1 = data[tiffStart + 1] & 0xFF;
        if (b0 == 'I' && b1 == 'I') {
            bigEndian = false;
        } else if (b0 == 'M' && b1 == 'M') {
            bigEndian = true;
        } else {
            return 1;
        }
        // Bytes 2-3 are the fixed 0x002A magic; byte 4-7 are the IFD0 offset from tiffStart.
        long ifdOffset = readUnsigned(data, tiffStart + 4, 4, bigEndian);
        long ifdStart = tiffStart + ifdOffset;
        if (ifdStart + 2 > limit || ifdStart < tiffStart) {
            return 1;
        }
        int entryCount = (int) readUnsigned(data, (int) ifdStart, 2, bigEndian);
        long entry = ifdStart + 2;
        for (int i = 0; i < entryCount; i++, entry += 12) {
            if (entry + 12 > limit) {
                return 1;
            }
            int tag = (int) readUnsigned(data, (int) entry, 2, bigEndian);
            if (tag == 0x0112) {
                // Orientation is a SHORT stored in the value field (first 2 bytes of entry+8).
                int value = (int) readUnsigned(data, (int) entry + 8, 2, bigEndian);
                if (value >= 1 && value <= 8) {
                    return value;
                }
                return 1;
            }
        }
        return 1;
    }

    /**
     * Reads an unsigned big- or little-endian integer of {@code length} bytes from {@code data}.
     */
    private long readUnsigned(byte[] data, int offset, int length, boolean bigEndian) {
        long value = 0;
        for (int i = 0; i < length; i++) {
            int b = data[offset + (bigEndian ? i : length - 1 - i)] & 0xFF;
            value = (value << 8) | b;
        }
        return value;
    }

    /**
     * Re-orients a decoded image to its upright display orientation per an EXIF Orientation value
     * (1..8), using a pure-JDK {@link AffineTransform}. Orientation {@code 1} (and anything out of
     * range) returns {@code src} unchanged, so already-upright images are untouched.
     *
     * <p>For orientation values 5..8 the image is rotated by 90°, which swaps width and height: the
     * returned image has dimensions {@code height x width} of the source. The caller uses these
     * post-rotation dimensions for the downscale math so the requested width and aspect ratio stay
     * correct.
     *
     * @param src          the decoded source image
     * @param orientation  the EXIF orientation value (1..8)
     * @param outputFormat the ImageIO format name ("jpg"/"png") controlling the destination type
     * @return a re-oriented image, or {@code src} for orientation 1
     */
    BufferedImage applyOrientation(BufferedImage src, int orientation, String outputFormat) {
        if (orientation <= 1 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swapDims = orientation >= 5;
        int destW = swapDims ? h : w;
        int destH = swapDims ? w : h;

        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2: // flip horizontal
                t.scale(-1.0, 1.0);
                t.translate(-w, 0);
                break;
            case 3: // rotate 180
                t.translate(w, h);
                t.rotate(Math.PI);
                break;
            case 4: // flip vertical
                t.scale(1.0, -1.0);
                t.translate(0, -h);
                break;
            case 5: // transpose (rotate 90 CW then flip horizontal)
                t.rotate(Math.PI / 2);
                t.scale(1.0, -1.0);
                break;
            case 6: // rotate 90 CW
                t.translate(h, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // transverse (reflect across the anti-diagonal; transpose + 180)
                t.rotate(Math.PI / 2);
                t.translate(0, -h);
                t.scale(-1.0, 1.0);
                t.translate(-w, 0);
                break;
            case 8: // rotate 90 CCW
                t.translate(0, w);
                t.rotate(-Math.PI / 2);
                break;
            default:
                return src;
        }

        int type = "png".equals(outputFormat) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dest = new BufferedImage(destW, destH, type);
        Graphics2D g = dest.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, t, null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    private AdvertisedUnitDTO toDto(Room room) {
        List<Long> imageIds = roomService.getRoomImages(room.getId()).stream()
                .map(GalleryImage::getId)
                .collect(Collectors.toList());
        Long propertyId = room.getProperty() != null ? room.getProperty().getId() : null;
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
                imageIds,
                propertyId,
                roomService.isLeaseAvailable(room),
                roomService.resolveStreetAddress(room));
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
