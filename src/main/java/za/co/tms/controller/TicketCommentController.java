package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.Ticket;
import za.co.tms.domain.TicketComment;
import za.co.tms.domain.UserRoles;
import za.co.tms.repository.AppUserRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TicketCommentService;
import za.co.tms.service.TicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
public class TicketCommentController {

    private final TicketCommentService ticketCommentService;
    private final TicketService ticketService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AppUserRepository appUserRepository;

    @Value("${app.admin.email:tshepomokgoatjane11@gmail.com}")
    private String adminEmail;

    @Autowired
    public TicketCommentController(TicketCommentService ticketCommentService,
                                   TicketService ticketService,
                                   EmailService emailService,
                                   SmsService smsService,
                                   AppUserRepository appUserRepository) {
        this.ticketCommentService = ticketCommentService;
        this.ticketService = ticketService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<TicketComment>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketCommentService.retrieveCommentsById(ticketId));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketComment> addComment(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        Ticket ticket = ticketService.findHelpdeskTicketById(ticketId);

        // Get current user info
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String role = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        // Create comment
        TicketComment ticketComment = new TicketComment();
        ticketComment.setTicket(ticket);
        ticketComment.setComment(body.get("comment"));
        ticketComment.setAuthor(username);
        ticketComment.setRole(UserRoles.valueOf(role));

        // Save comment
        TicketComment savedComment = ticketCommentService.createComment(ticketId, ticketComment);

        // Send notifications
        try {
            if (role.equals("ADMIN")) {
                // Admin replied → notify tenant via email
                appUserRepository.findByUsername(ticket.getRaisedBy())
                    .ifPresent(user -> emailService.send(
                        user.getEmail(),
                        "Ticket #" + ticket.getTicketNumber() + " Updated",
                        "Your maintenance ticket has been updated. Log in to view the response."
                    )
                );            
            } else {
                // Tenant replied → notify admin
                emailService.send(
                        adminEmail,
                        "New message on Ticket #" + ticket.getTicketNumber(),
                        "Tenant " + username + " has replied on ticket #" + ticket.getTicketNumber()
                );
            }
        } catch (Exception e) {
            // Don't fail the request if notification fails
            // Log the error but still return the saved comment
        }

        return ResponseEntity.ok(savedComment);
    }
}
