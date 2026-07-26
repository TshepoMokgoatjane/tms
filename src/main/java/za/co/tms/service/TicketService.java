package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import za.co.tms.domain.Ticket;
import za.co.tms.domain.Status;
import za.co.tms.domain.TicketComment;
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.TicketRepository;

@Slf4j
@Service
public class TicketService {

	private final TicketRepository ticketRepository;
	private final EmailService emailService;
	private final SmsService smsService;
	private final TicketCommentService ticketCommentService;
	
	@Autowired
	public TicketService(TicketRepository ticketRepository, EmailService emailService, SmsService smsService, TicketCommentService ticketCommentService) {
		this.ticketRepository = ticketRepository;
		this.emailService = emailService;
		this.smsService = smsService;
		this.ticketCommentService = ticketCommentService;
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
		String previousComments = existingTicket.getComments();
		String newComments = helpdeskTicket.getComments();
		
		// Get current user info for comment tracking
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		String role = SecurityContextHolder.getContext().getAuthentication()
				.getAuthorities().stream().findFirst()
				.map(a -> a.getAuthority().replace("ROLE_", ""))
				.orElse("USER");
		
		// Update the ticket
		ticketRepository.save(helpdeskTicket);
		
		// Track comment changes in ticket_comment table (for history)
		if (newComments != null && !newComments.equals(previousComments) && !newComments.isBlank()) {
			log.info("Ticket #{} comments updated, creating history entry", helpdeskTicket.getTicketNumber());
			TicketComment ticketComment = new TicketComment();
			ticketComment.setTicket(helpdeskTicket);
			ticketComment.setComment(newComments);
			ticketComment.setAuthor(username);
			ticketComment.setRole(UserRoles.valueOf(role));
			ticketCommentService.createComment(helpdeskTicket.getId(), ticketComment);
		}
		
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
