package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpdeskTicketManagement extends JpaRepository<HelpdeskTicketManagement, Integer> {

	List<HelpdeskTicketManagement> findHelpdeskTicketManagementByTicketNumber(int ticketNumber);
	List<HelpdeskTicketManagement> findHelpdeskTicketManagementById(int id);
	List<HelpdeskTicketManagement> findHelpdeskTicketManagementByRaisedBy(String raisedBy);	
}
