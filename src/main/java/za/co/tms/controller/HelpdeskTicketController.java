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

import za.co.tms.model.HelpdeskTicket;
import za.co.tms.service.HelpdeskTicketService;

@RestController
@RequestMapping("/helpDeskTicketManagement")
public class HelpdeskTicketController {
	
	private HelpdeskTicketService helpDeskTicketService;
	
	@Autowired
	public HelpdeskTicketController(HelpdeskTicketService helpDeskTicketService) {
		this.helpDeskTicketService = helpDeskTicketService;
	}

	@GetMapping(path="/find/all")
	public List<HelpdeskTicket> retrieveHelpdeskTickets() {
		return helpDeskTicketService.findAllHelpdeskTickets();
	}
	
	@GetMapping(path="/find/by/{id}")
	public HelpdeskTicket retrieveHelpdeskTicketById(@PathVariable int id) {
		return helpDeskTicketService.findHelpdeskTicketById(id);
	}
	
	@GetMapping(path="/find/by/{ticketNumber}")
	public HelpdeskTicket retrieveHelpdeskTicketManagementByTicketNumber(@PathVariable int ticketNumber) {
		return helpDeskTicketService.findHelpdeskTicketByTicketNumber(ticketNumber);
	}
	
	@GetMapping(path="/find/by/{raisedBy}")
	public HelpdeskTicket retrieveHelpdeskTicketByRaisedBy(@PathVariable String raisedBy) {
		return helpDeskTicketService.findHelpdeskTicketByRaisedBy(raisedBy);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteHelpdeskTicketManagementById(@PathVariable int id) {
		helpDeskTicketService.deleteHelpdeskTicketById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public HelpdeskTicket updateHelpdeskTicket(@PathVariable int id, @RequestBody HelpdeskTicket helpDeskTicket) {
		helpDeskTicketService.updateHelpdeskTicket(helpDeskTicket);
		return helpDeskTicket;
	}
	
	@PostMapping("/create")
	public HelpdeskTicket createHelpdeskTicketManagement(@RequestBody HelpdeskTicket helpDeskTicket) {
		HelpdeskTicket createdHelpdeskTicket = helpDeskTicketService.addHelpdeskTicket(helpDeskTicket);
		return createdHelpdeskTicket;
	}
}
