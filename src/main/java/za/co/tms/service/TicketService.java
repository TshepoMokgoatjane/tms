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

	private TicketRepository ticketRepository;
	
	@Autowired
	public TicketService(TicketRepository ticketRepository) {
		this.ticketRepository = ticketRepository;
	}
	
	public List<Ticket> findAllHelpdeskTickets() {
		return ticketRepository.findAll();
	}
	
	public Ticket findHelpdeskTicketById(int id) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getId() == id;
		Ticket HelpdeskTicket = ticketRepository.findTicketById(id).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket findHelpdeskTicketByTicketNumber(int ticketNumber) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getTicketNumber() == ticketNumber;
		Ticket HelpdeskTicket = ticketRepository.findTicketByTicketNumber(ticketNumber).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket findHelpdeskTicketByRaisedBy(String raisedBy) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getRaisedBy().equalsIgnoreCase(raisedBy);
		Ticket HelpdeskTicket = ticketRepository.findTicketByRaisedBy(raisedBy).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket addHelpdeskTicket(Ticket helpdeskTicket) {
		
		helpdeskTicket.setId(null);
		
		return ticketRepository.save(helpdeskTicket);
	}
	
	public void deleteHelpdeskTicketById(int id) {
		Ticket helpdeskTicket = findHelpdeskTicketById(id);
		helpdeskTicket.setStatus(Status.CLOSED);
		ticketRepository.save(helpdeskTicket);
	}
	
	public void updateHelpdeskTicket(Ticket helpdeskTicket) {
		deleteHelpdeskTicketById(helpdeskTicket.getId());
		ticketRepository.save(helpdeskTicket);
	}
}
