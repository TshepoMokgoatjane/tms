package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

	List<Ticket> findHelpdeskTicketByTicketNumber(int ticketNumber);
	List<Ticket> findHelpdeskTicketById(int id);
	List<Ticket> findHelpdeskTicketByRaisedBy(String raisedBy);	
}
