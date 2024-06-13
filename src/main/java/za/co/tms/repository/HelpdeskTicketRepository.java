package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.HelpdeskTicket;

public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Integer> {

	List<HelpdeskTicket> findHelpdeskTicketByTicketNumber(int ticketNumber);
	List<HelpdeskTicket> findHelpdeskTicketById(int id);
	List<HelpdeskTicket> findHelpdeskTicketByRaisedBy(String raisedBy);	
}
