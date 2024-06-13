package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.model.HelpdeskTicketManagement;
import za.co.tms.model.Status;
import za.co.tms.repository.HelpdeskTicketManagementRepository;

@Service
public class HelpdeskTicketManagementService {

	private HelpdeskTicketManagementRepository helpDeskTicketManagementRepository;
	
	@Autowired
	public HelpdeskTicketManagementService(HelpdeskTicketManagementRepository helpDeskTicketManagementRepository) {
		this.helpDeskTicketManagementRepository = helpDeskTicketManagementRepository;
	}
	
	public List<HelpdeskTicketManagement> findAllHelpdeskTicketManagement() {
		return helpDeskTicketManagementRepository.findAll();
	}
	
	public HelpdeskTicketManagement findHelpdeskTicketManagementById(int id) {
		Predicate<? super HelpdeskTicketManagement> predicate = helpDeskTicketManagement -> helpDeskTicketManagement.getId() == id;
		HelpdeskTicketManagement helpDeskTicketManagement = helpDeskTicketManagementRepository.findHelpdeskTicketManagementById(id).stream().filter(predicate).findFirst().get();
		return helpDeskTicketManagement;
	}
	
	public HelpdeskTicketManagement findHelpdeskTicketManagementByTicketNumber(int ticketNumber) {
		Predicate<? super HelpdeskTicketManagement> predicate = helpDeskTicketManagement -> helpDeskTicketManagement.getTicketNumber() == ticketNumber;
		HelpdeskTicketManagement helpDeskTicketManagement = helpDeskTicketManagementRepository.findHelpdeskTicketManagementByTicketNumber(ticketNumber).stream().filter(predicate).findFirst().get();
		return helpDeskTicketManagement;
	}
	
	public HelpdeskTicketManagement findHelpdeskTicketManagementByRaisedBy(String raisedBy) {
		Predicate<? super HelpdeskTicketManagement> predicate = helpDeskTicketManagement -> helpDeskTicketManagement.getRaisedBy().equalsIgnoreCase(raisedBy);
		HelpdeskTicketManagement helpDeskTicketManagement = helpDeskTicketManagementRepository.findHelpdeskTicketManagementByRaisedBy(raisedBy).stream().filter(predicate).findFirst().get();
		return helpDeskTicketManagement;
	}
	
	public HelpdeskTicketManagement addHelpDeskTicketManagement(HelpdeskTicketManagement helpDeskTicketManagement) {
		
		helpDeskTicketManagement.setId(null);
		
		return helpDeskTicketManagementRepository.save(helpDeskTicketManagement);
	}
	
	public void deleteHelpdeskTicketManagementById(int id) {
		HelpdeskTicketManagement helpDeskTicketManagement = findHelpdeskTicketManagementById(id);
		helpDeskTicketManagement.setStatus(Status.CLOSED);
		helpDeskTicketManagementRepository.save(helpDeskTicketManagement);
	}
	
	public void updateHelpdeskTicketManagement(HelpdeskTicketManagement helpDeskTicketManagement) {
		deleteHelpdeskTicketManagementById(helpDeskTicketManagement.getId());
		helpDeskTicketManagementRepository.save(helpDeskTicketManagement);
	}
}
