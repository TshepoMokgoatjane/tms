package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.TicketImage;

import java.util.List;

public interface TicketImageRepository extends JpaRepository<TicketImage, Long> {

    List<TicketImage> findByTicketIdOrderByUploadedAtAscIdAsc(Long ticketId);

    long countByTicketId(Long ticketId);

    void deleteByTicketId(Long ticketId);
}
