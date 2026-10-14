package za.co.tms.repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.Status;
import za.co.tms.domain.Ticket;
public interface TicketRepository extends JpaRepository<Ticket, Long> {
	List<Ticket> findTicketById(Long id);
	List<Ticket> findByRaisedBy(String raisedBy);

	/**
	 * Tickets in a terminal state (CLOSED/COMPLETED) whose status last changed on or before the
	 * given cutoff. Used by the retention job. Tickets with a null statusChangedAt are excluded
	 * (they are treated as recently touched and left alone until a status change stamps them).
	 */
	List<Ticket> findByStatusInAndStatusChangedAtLessThanEqual(Collection<Status> statuses, LocalDateTime cutoff);
}