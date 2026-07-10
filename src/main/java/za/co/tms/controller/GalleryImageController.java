package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/gallery")
public class GalleryImageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GalleryImageController.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final GalleryImageService galleryImageService;
    private final Path uploadDir;

    @Autowired
    public GalleryImageController(GalleryImageService galleryImageService,
                                  @Value("${gallery.upload.dir:uploads/gallery}") String uploadDirPath) {
        this.galleryImageService = galleryImageService;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            LOGGER.warn("Could not create gallery upload directory: {}. File uploads will fail until directory is available.", uploadDir);
        }
    }

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping("/active")
    @Operation(summary = "Get active gallery images", description = "Returns only active images for the public gallery")
    public ResponseEntity<List<GalleryImage>> getActiveImages() {
        return ResponseEntity.ok(galleryImageService.getActiveGalleryImages());
    }

    @GetMapping("/image/{filename}")
    @Operation(summary = "Serve an image file", description = "Returns the actual image binary for display")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();

            // Security: prevent path traversal
            if (!filePath.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);
        } catch (IOException e) {
            LOGGER.error("Error serving image {}: {}", filename, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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
    @Operation(summary = "Upload a new image", description = "Accepts multipart file upload with metadata")
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
            // Generate unique filename
            String extension = getFileExtension(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extension;

            // Save file to disk
            Path targetPath = uploadDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Build entity
            GalleryImage image = new GalleryImage();
            image.setTitle(title);
            image.setDescription(description);
            image.setFilename(storedFilename);
            image.setOriginalFilename(file.getOriginalFilename());
            image.setContentType(contentType);
            image.setFileSize(file.getSize());
            image.setCategory(GalleryImageCategory.valueOf(category.toUpperCase()));
            image.setStatus(GalleryImageStatus.ACTIVE);
            image.setDisplayOrder(0);

            GalleryImage saved = galleryImageService.createGalleryImage(image);

            LOGGER.info("Image uploaded: {} -> {}", file.getOriginalFilename(), storedFilename);

            return ResponseEntity.ok(saved);
        } catch (IOException e) {
            LOGGER.error("Failed to store uploaded file: {}", e.getMessage());
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
    @Operation(summary = "Delete an image", description = "Permanently removes the image record and file")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        GalleryImage image = galleryImageService.getGalleryImageById(id);

        // Delete file from disk
        try {
            Path filePath = uploadDir.resolve(image.getFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            LOGGER.warn("Could not delete file {} from disk: {}", image.getFilename(), e.getMessage());
        }

        galleryImageService.deleteGalleryImage(id);
        return ResponseEntity.noContent().build();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
