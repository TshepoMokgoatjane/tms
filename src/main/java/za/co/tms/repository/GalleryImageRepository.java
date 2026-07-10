package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageStatus;

import java.util.List;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
    List<GalleryImage> findByStatusOrderByDisplayOrderAsc(GalleryImageStatus status);
}
