package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.model.HelpdeskTicket;
import za.co.tms.model.Status;
import za.co.tms.repository.HelpdeskTicketRepository;

@Service
public class HelpdeskTicketService {

	private HelpdeskTicketRepository helpdeskTicketRepository;
	
	@Autowired
	public HelpdeskTicketService(HelpdeskTicketRepository helpdeskTicketRepository) {
		this.helpdeskTicketRepository = helpdeskTicketRepository;
	}
	
	public List<HelpdeskTicket> findAllHelpdeskTickets() {
		return helpdeskTicketRepository.findAll();
	}
	
	public HelpdeskTicket findHelpdeskTicketById(int id) {
		Predicate<? super HelpdeskTicket> predicate = helpdeskTicket -> helpdeskTicket.getId() == id;
		HelpdeskTicket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketById(id).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public HelpdeskTicket findHelpdeskTicketByTicketNumber(int ticketNumber) {
		Predicate<? super HelpdeskTicket> predicate = helpdeskTicket -> helpdeskTicket.getTicketNumber() == ticketNumber;
		HelpdeskTicket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketByTicketNumber(ticketNumber).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public HelpdeskTicket findHelpdeskTicketByRaisedBy(String raisedBy) {
		Predicate<? super HelpdeskTicket> predicate = helpdeskTicket -> helpdeskTicket.getRaisedBy().equalsIgnoreCase(raisedBy);
		HelpdeskTicket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketByRaisedBy(raisedBy).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public HelpdeskTicket addHelpdeskTicket(HelpdeskTicket helpdeskTicket) {
		
		helpdeskTicket.setId(null);
		
		return helpdeskTicketRepository.save(helpdeskTicket);
	}
	
	public void deleteHelpdeskTicketById(int id) {
		HelpdeskTicket helpdeskTicket = findHelpdeskTicketById(id);
		helpdeskTicket.setStatus(Status.CLOSED);
		helpdeskTicketRepository.save(helpdeskTicket);
	}
	
	public void updateHelpdeskTicket(HelpdeskTicket helpdeskTicket) {
		deleteHelpdeskTicketById(helpdeskTicket.getId());
		helpdeskTicketRepository.save(helpdeskTicket);
	}
}
