package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.model.Ticket;
import za.co.tms.model.Status;
import za.co.tms.repository.TicketRepository;

@Service
public class TicketService {

	private TicketRepository helpdeskTicketRepository;
	
	@Autowired
	public TicketService(TicketRepository helpdeskTicketRepository) {
		this.helpdeskTicketRepository = helpdeskTicketRepository;
	}
	
	public List<Ticket> findAllHelpdeskTickets() {
		return helpdeskTicketRepository.findAll();
	}
	
	public Ticket findHelpdeskTicketById(int id) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getId() == id;
		Ticket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketById(id).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket findHelpdeskTicketByTicketNumber(int ticketNumber) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getTicketNumber() == ticketNumber;
		Ticket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketByTicketNumber(ticketNumber).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket findHelpdeskTicketByRaisedBy(String raisedBy) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getRaisedBy().equalsIgnoreCase(raisedBy);
		Ticket HelpdeskTicket = helpdeskTicketRepository.findHelpdeskTicketByRaisedBy(raisedBy).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket addHelpdeskTicket(Ticket helpdeskTicket) {
		
		helpdeskTicket.setId(null);
		
		return helpdeskTicketRepository.save(helpdeskTicket);
	}
	
	public void deleteHelpdeskTicketById(int id) {
		Ticket helpdeskTicket = findHelpdeskTicketById(id);
		helpdeskTicket.setStatus(Status.CLOSED);
		helpdeskTicketRepository.save(helpdeskTicket);
	}
	
	public void updateHelpdeskTicket(Ticket helpdeskTicket) {
		deleteHelpdeskTicketById(helpdeskTicket.getId());
		helpdeskTicketRepository.save(helpdeskTicket);
	}
}
