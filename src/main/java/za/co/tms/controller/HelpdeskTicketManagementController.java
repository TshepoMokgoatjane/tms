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

import za.co.tms.model.HelpdeskTicketManagement;
import za.co.tms.service.HelpdeskTicketManagementService;

@RestController
@RequestMapping("/helpDeskTicketManagement")
public class HelpdeskTicketManagementController {
	
	private HelpdeskTicketManagementService helpDeskTicketManagementService;
	
	@Autowired
	public HelpdeskTicketManagementController(HelpdeskTicketManagementService helpDeskTicketManagementService) {
		this.helpDeskTicketManagementService = helpDeskTicketManagementService;
	}

	@GetMapping(path="/find/all")
	public List<HelpdeskTicketManagement> retrieveHelpdeskTickets() {
		return helpDeskTicketManagementService.findAllHelpdeskTicketManagement();
	}
	
	@GetMapping(path="/find/by/{id}")
	public HelpdeskTicketManagement retrieveHelpdeskTicketManagementById(@PathVariable int id) {
		return helpDeskTicketManagementService.findHelpdeskTicketManagementById(id);
	}
	
	@GetMapping(path="/find/by/{ticketNumber}")
	public HelpdeskTicketManagement retrieveHelpdeskTicketManagementByTicketNumber(@PathVariable int ticketNumber) {
		return helpDeskTicketManagementService.findHelpdeskTicketManagementByTicketNumber(ticketNumber);
	}
	
	@GetMapping(path="/find/by/{raisedBy}")
	public HelpdeskTicketManagement retrieveHelpdeskTicketManagementByRaisedBy(@PathVariable String raisedBy) {
		return helpDeskTicketManagementService.findHelpdeskTicketManagementByRaisedBy(raisedBy);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteHelpdeskTicketManagementById(@PathVariable int id) {
		helpDeskTicketManagementService.deleteHelpdeskTicketManagementById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public HelpdeskTicketManagement updateHelpdeskTicketManagement(@PathVariable int id, @RequestBody HelpdeskTicketManagement helpDeskTicketManagement) {
		helpDeskTicketManagementService.updateHelpdeskTicketManagement(helpDeskTicketManagement);
		return helpDeskTicketManagement;
	}
	
	@PostMapping("/create")
	public HelpdeskTicketManagement createHelpdeskTicketManagement(@RequestBody HelpdeskTicketManagement helpDeskTicketManagement) {
		HelpdeskTicketManagement createdHelpdeskTicketManagement = helpDeskTicketManagementService.addHelpDeskTicketManagement(helpDeskTicketManagement);
		return createdHelpdeskTicketManagement;
	}
}
