package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageStatus;
import za.co.tms.exception.GalleryImageNotFoundException;
import za.co.tms.repository.GalleryImageRepository;

import java.util.List;

@Service
public class GalleryImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GalleryImageService.class);

    private final GalleryImageRepository galleryImageRepository;

    @Autowired
    public GalleryImageService(GalleryImageRepository galleryImageRepository) {
        this.galleryImageRepository = galleryImageRepository;
    }

    public List<GalleryImage> getAllGalleryImages() {
        LOGGER.info("Find all gallery images");
        return galleryImageRepository.findAll();
    }

    public List<GalleryImage> getActiveGalleryImages() {
        LOGGER.info("Find all active gallery images");
        return galleryImageRepository.findByStatusOrderByDisplayOrderAsc(GalleryImageStatus.ACTIVE);
    }

    public GalleryImage getGalleryImageById(Long id) {
        LOGGER.info("Find gallery image by id: {}", id);
        return galleryImageRepository.findById(id)
                .orElseThrow(() -> new GalleryImageNotFoundException(id));
    }

    public GalleryImage createGalleryImage(GalleryImage galleryImage) {
        LOGGER.info("Create gallery image: {}", galleryImage.getOriginalFilename());
        return galleryImageRepository.save(galleryImage);
    }

    public GalleryImage updateGalleryImage(Long id, GalleryImage updatedGalleryImage) {
        GalleryImage existing = getGalleryImageById(id);

        existing.setTitle(updatedGalleryImage.getTitle());
        existing.setDescription(updatedGalleryImage.getDescription());
        existing.setCategory(updatedGalleryImage.getCategory());
        existing.setDisplayOrder(updatedGalleryImage.getDisplayOrder());

        GalleryImage saved = galleryImageRepository.save(existing);
        LOGGER.info("Gallery image updated: id={}", id);
        return saved;
    }

    public void changeGalleryImageStatus(Long id, GalleryImageStatus status) {
        GalleryImage galleryImage = getGalleryImageById(id);
        GalleryImageStatus previousStatus = galleryImage.getStatus();

        galleryImage.setStatus(status);
        galleryImageRepository.save(galleryImage);

        LOGGER.info("Gallery image status changed: id={}, from={}, to={}", id, previousStatus, status);
    }

    public void deleteGalleryImage(Long id) {
        GalleryImage galleryImage = getGalleryImageById(id);
        galleryImageRepository.delete(galleryImage);
        LOGGER.info("Gallery image permanently deleted: id={}", id);
    }
}
