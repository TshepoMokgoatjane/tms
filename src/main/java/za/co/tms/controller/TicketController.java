package za.co.tms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.model.Ticket;
import za.co.tms.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {
	
	private TicketService ticketService;
	
	@Autowired
	public TicketController(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping(path="/find/all")
	public List<Ticket> retrieveHelpdeskTickets() {
		return ticketService.findAllHelpdeskTickets();
	}
	
	@GetMapping(path="/find/by/{id}")
	public Ticket retrieveHelpdeskTicketById(@PathVariable int id) {
		return ticketService.findHelpdeskTicketById(id);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteHelpdeskTicketManagementById(@PathVariable int id) {
		ticketService.deleteHelpdeskTicketById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public Ticket updateHelpdeskTicket(@PathVariable int id, @RequestBody Ticket helpDeskTicket) {
		ticketService.updateHelpdeskTicket(helpDeskTicket);
		return helpDeskTicket;
	}
	
	@PostMapping("/create")
	public Ticket createHelpdeskTicketManagement(@RequestBody Ticket helpDeskTicket) {
		Ticket createdHelpdeskTicket = ticketService.addHelpdeskTicket(helpDeskTicket);
		return createdHelpdeskTicket;
	}
}
