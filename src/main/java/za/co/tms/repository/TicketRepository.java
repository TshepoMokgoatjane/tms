package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

	List<Ticket> findTicketByTicketNumber(int ticketNumber);
	List<Ticket> findTicketById(int id);
	List<Ticket> findTicketByRaisedBy(String raisedBy);	
}
