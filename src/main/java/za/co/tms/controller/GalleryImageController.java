package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageCategory;
import za.co.tms.domain.GalleryImageStatus;
import za.co.tms.service.GalleryImageService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/gallery")
public class GalleryImageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GalleryImageController.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final GalleryImageService galleryImageService;

    @Autowired
    public GalleryImageController(GalleryImageService galleryImageService) {
        this.galleryImageService = galleryImageService;
    }

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping("/active")
    @Operation(summary = "Get active gallery images", description = "Returns only active images for the public gallery")
    public ResponseEntity<List<GalleryImage>> getActiveImages() {
        return ResponseEntity.ok(galleryImageService.getActiveGalleryImages());
    }

    @GetMapping("/image/{id}")
    @Operation(summary = "Serve an image by ID", description = "Returns the actual image binary from the database")
    public ResponseEntity<byte[]> serveImage(@PathVariable Long id) {
        GalleryImage image = galleryImageService.getGalleryImageById(id);

        if (image.getImageData() == null || image.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentType = image.getContentType() != null ? image.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(image.getImageData());
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/find/all")
    @Operation(summary = "Get all gallery images (admin)", description = "Returns all images regardless of status")
    public ResponseEntity<List<GalleryImage>> getAllImages() {
        return ResponseEntity.ok(galleryImageService.getAllGalleryImages());
    }

    @GetMapping("/find/by/{id}")
    @Operation(summary = "Get gallery image by ID")
    public ResponseEntity<GalleryImage> getImageById(@PathVariable Long id) {
        return ResponseEntity.ok(galleryImageService.getGalleryImageById(id));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a new image", description = "Accepts multipart file upload with metadata, stores in database")
    public ResponseEntity<GalleryImage> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", defaultValue = "PROPERTY") String category) {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            LOGGER.warn("Rejected upload: unsupported content type {}", contentType);
            return ResponseEntity.badRequest().build();
        }

        try {
            // Build entity with image data stored in DB
            GalleryImage image = new GalleryImage();
            image.setTitle(title);
            image.setDescription(description);
            image.setOriginalFilename(file.getOriginalFilename());
            image.setContentType(contentType);
            image.setFileSize(file.getSize());
            image.setImageData(file.getBytes());
            image.setCategory(GalleryImageCategory.valueOf(category.toUpperCase()));
            image.setStatus(GalleryImageStatus.ACTIVE);
            image.setDisplayOrder(0);

            GalleryImage saved = galleryImageService.createGalleryImage(image);

            LOGGER.info("Image uploaded to database: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            LOGGER.error("Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update image metadata", description = "Update title, description, category, or display order")
    public ResponseEntity<GalleryImage> updateImage(@PathVariable Long id, @RequestBody GalleryImage updatedImage) {
        GalleryImage updated = galleryImageService.updateGalleryImage(id, updatedImage);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate an image", description = "Makes the image visible on the public gallery")
    public ResponseEntity<Void> activateImage(@PathVariable Long id) {
        galleryImageService.changeGalleryImageStatus(id, GalleryImageStatus.ACTIVE);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an image", description = "Hides the image from the public gallery")
    public ResponseEntity<Void> deactivateImage(@PathVariable Long id) {
        galleryImageService.changeGalleryImageStatus(id, GalleryImageStatus.INACTIVE);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an image", description = "Permanently removes the image record from the database")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        galleryImageService.deleteGalleryImage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/migrate/{id}")
    @Operation(summary = "Migrate image data (temporary)", description = "Uploads image bytes for an existing gallery image record")
    public ResponseEntity<String> migrateImageData(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            GalleryImage image = galleryImageService.getGalleryImageById(id);
            image.setImageData(file.getBytes());
            image.setContentType(file.getContentType());
            image.setFileSize(file.getSize());
            galleryImageService.createGalleryImage(image);

            LOGGER.info("Migration: Populated image_data for id={} ({} bytes)", id, file.getSize());
            return ResponseEntity.ok("Migrated image id=" + id);
        } catch (IOException e) {
            LOGGER.error("Migration: Failed for id={}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}
