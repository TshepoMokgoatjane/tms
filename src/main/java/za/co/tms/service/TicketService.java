package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.domain.Ticket;
import za.co.tms.domain.Status;
import za.co.tms.repository.TicketRepository;

@Slf4j
@Service
public class TicketService {

	private final TicketRepository ticketRepository;
	private final EmailService emailService;
	private final SmsService smsService;
	
	@Autowired
	public TicketService(TicketRepository ticketRepository, EmailService emailService, SmsService smsService) {
		this.ticketRepository = ticketRepository;
		this.emailService = emailService;
		this.smsService = smsService;
	}
	
	public List<Ticket> findAllHelpdeskTickets() {
		return ticketRepository.findAll();
	}
	
	public Ticket findHelpdeskTicketById(Long id) {
		Predicate<? super Ticket> predicate = helpdeskTicket -> helpdeskTicket.getId() == id;
		Ticket HelpdeskTicket = ticketRepository.findTicketById(id).stream().filter(predicate).findFirst().get();
		return HelpdeskTicket;
	}
	
	public Ticket addHelpdeskTicket(Ticket helpdeskTicket) {
		helpdeskTicket.setId(null);
		Ticket savedTicket = ticketRepository.save(helpdeskTicket);
		
		// Send notifications when ticket is created
		log.info("Sending ticket creation notifications for ticket #{}", savedTicket.getTicketNumber());
		emailService.sendTicketCreatedNotification(savedTicket);
		smsService.sendTicketCreatedSms(savedTicket);
		
		return savedTicket;
	}
	
	public void deleteHelpdeskTicketById(Long id) {
		Ticket helpdeskTicket = findHelpdeskTicketById(id);
		helpdeskTicket.setStatus(Status.CLOSED);
		ticketRepository.save(helpdeskTicket);
	}
	
	public void updateHelpdeskTicket(Ticket helpdeskTicket) {
		// Fetch the existing ticket to compare status
		Ticket existingTicket = findHelpdeskTicketById(helpdeskTicket.getId());
		Status previousStatus = existingTicket.getStatus();
		
		// Update the ticket
		ticketRepository.save(helpdeskTicket);
		
		// Send notifications if status has changed and ticket is not deleted
		if (previousStatus != helpdeskTicket.getStatus() && helpdeskTicket.getStatus() != Status.CLOSED) {
			log.info("Ticket #{} status changed from {} to {}, sending notifications", 
					helpdeskTicket.getTicketNumber(), previousStatus, helpdeskTicket.getStatus());
			emailService.sendTicketStatusUpdateNotification(helpdeskTicket, previousStatus.name());
			smsService.sendTicketStatusUpdateSms(helpdeskTicket, previousStatus.name());
		} else if (helpdeskTicket.getStatus() == Status.CLOSED) {
			log.info("Ticket #{} has been closed, sending closure notifications", helpdeskTicket.getTicketNumber());
			emailService.sendTicketStatusUpdateNotification(helpdeskTicket, previousStatus.name());
			smsService.sendTicketStatusUpdateSms(helpdeskTicket, previousStatus.name());
		}
	}
}
