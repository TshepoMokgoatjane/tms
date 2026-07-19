package za.co.tms.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.Ticket;
public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findTicketById(Long id);
	List<Ticket> findByRaisedBy(String raisedBy);
}