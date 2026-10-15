package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tms.domain.GalleryImage;
import za.co.tms.domain.GalleryImageStatus;

import java.util.List;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {

    // ===== Metadata-only projections (NEVER select image_data) =====
    // These power list views. Loading the blob for every row previously caused OutOfMemoryError.
    // The constructor expression maps to GalleryImage's metadata-only constructor, leaving
    // imageData null; the actual bytes are only loaded when a single image is fetched by id.

    @Query("""
            select new za.co.tms.domain.GalleryImage(
                g.id, g.title, g.description, g.originalFilename, g.contentType, g.fileSize,
                g.category, g.status, g.displayOrder, g.roomId)
            from GalleryImage g
            where g.status = :status
            order by g.displayOrder asc""")
    List<GalleryImage> findActiveMetadata(@Param("status") GalleryImageStatus status);

    @Query("""
            select new za.co.tms.domain.GalleryImage(
                g.id, g.title, g.description, g.originalFilename, g.contentType, g.fileSize,
                g.category, g.status, g.displayOrder, g.roomId)
            from GalleryImage g
            order by g.displayOrder asc""")
    List<GalleryImage> findAllMetadata();

    // Photos for a specific advertised unit, ordered for a carousel — metadata only.
    @Query("""
            select new za.co.tms.domain.GalleryImage(
                g.id, g.title, g.description, g.originalFilename, g.contentType, g.fileSize,
                g.category, g.status, g.displayOrder, g.roomId)
            from GalleryImage g
            where g.roomId = :roomId
            order by g.displayOrder asc, g.id asc""")
    List<GalleryImage> findByRoomIdMetadata(@Param("roomId") Long roomId);

    // Bulk delete all photos for a room without loading them into memory.
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from GalleryImage g where g.roomId = :roomId")
    void deleteByRoomId(@Param("roomId") Long roomId);
}
