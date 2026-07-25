package za.co.tms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.domain.Ticket;
import za.co.tms.repository.TicketRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final EmailService emailService;
	private final SmsService smsService;

	@Autowired
	public TicketController(TicketService ticketService, TicketRepository ticketRepository, EmailService emailService, SmsService smsService) {
		this.ticketService = ticketService;
		this.ticketRepository = ticketRepository;
		this.emailService = emailService;
		this.smsService = smsService;
	}

	@GetMapping(path="/find/all")
	public List<Ticket> retrieveHelpdeskTickets() {
		return ticketService.findAllHelpdeskTickets();
	}

	@GetMapping(path="/find/my-tickets")
	public List<Ticket> retrieveMyTickets() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return ticketRepository.findByRaisedBy(username);
	}

	@GetMapping(path="/find/by/{id}")
	public Ticket retrieveHelpdeskTicketById(@PathVariable Long id) {
		return ticketService.findHelpdeskTicketById(id);
	}

	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteHelpdeskTicketManagementById(@PathVariable Long id) {
		ticketService.deleteHelpdeskTicketById(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping(path="/update/{id}")
	public Ticket updateHelpdeskTicket(@PathVariable Long id, @RequestBody Ticket helpDeskTicket) {
		helpDeskTicket.setId(id);
		ticketService.updateHelpdeskTicket(helpDeskTicket);
		return helpDeskTicket;
	}

	@PostMapping("/create")
	public Ticket createHelpdeskTicketManagement(@RequestBody Ticket helpDeskTicket) {
		Ticket createdHelpdeskTicket = ticketService.addHelpdeskTicket(helpDeskTicket);
		return createdHelpdeskTicket;
	}
}