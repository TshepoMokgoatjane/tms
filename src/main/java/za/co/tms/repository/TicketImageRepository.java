package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.tms.domain.TicketImage;

import java.util.List;

public interface TicketImageRepository extends JpaRepository<TicketImage, Long> {

    // Metadata only (never selects image_data) — powers the per-ticket photo list.
    @Query("""
            select new za.co.tms.domain.TicketImage(
                t.id, t.ticketId, t.originalFilename, t.contentType, t.fileSize, t.uploadedAt)
            from TicketImage t
            where t.ticketId = :ticketId
            order by t.uploadedAt asc, t.id asc""")
    List<TicketImage> findByTicketIdMetadata(@Param("ticketId") Long ticketId);

    long countByTicketId(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
