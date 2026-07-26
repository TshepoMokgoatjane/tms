package za.co.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import za.co.tms.domain.ContactUs;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.Ticket;
import za.co.tms.repository.AppUserRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${brevo.api.key:no-key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:noreply@tltproperties.co.za}")
    private String senderEmail;

    @Value("${brevo.sender.name:TLT Properties}")
    private String senderName;

    @Value("${contact.notification.email}")
    private String contactNotificationEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AppUserRepository appUserRepository;

    @Autowired
    public EmailService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Async
    public void sendContactUsNotification(ContactUs contactUs) {
        String subject = "New Contact Us Submission from " + contactUs.getFirstName() + " " + contactUs.getLastName();
        String body = String.format(
                "You have received a new Contact Us submission:<br><br>" +
                        "<b>Name:</b> %s %s<br>" +
                        "<b>Email:</b> %s<br>" +
                        "<b>Phone:</b> %s<br>" +
                        "<b>Heard about us via:</b> %s<br><br>" +
                        "<b>Message:</b><br>%s",
                contactUs.getFirstName(),
                contactUs.getLastName(),
                contactUs.getEmailAddress() != null ? contactUs.getEmailAddress() : "Not provided",
                contactUs.getMobilePhoneNumber() != null ? contactUs.getMobilePhoneNumber() : "Not provided",
                contactUs.getWhereDidYouHearAboutUs() != null ? contactUs.getWhereDidYouHearAboutUs().name() : "Not specified",
                contactUs.getMessage() != null ? contactUs.getMessage() : "No message"
        );

        send(contactNotificationEmail, subject, body);
    }

    @Async
    public void sendRentReminder(Tenant tenant) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            log.warn("Tenant {} {} has no email address, skipping email", tenant.getName(), tenant.getSurname());
            return;
        }

        String subject = "Rent Payment Reminder";
        String body = String.format(
                "Dear %s %s,<br><br>This is a friendly reminder that your rent is due today (%s).<br>" +
                        "<b>Room:</b> %s (%s)<br>" +
                        "<b>Meter Number:</b> %s<br><br>" +
                        "Thank you,<br>TLT Properties Management",
                tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                tenant.getSurname(),
                tenant.getPaymentDay().getLabel(),
                tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A",
                tenant.getRoom() != null ? tenant.getRoom().getDescription() : "N/A",
                tenant.getRoom() != null ? tenant.getRoom().getPrepaidElectricityMeterNumber() : "N/A"
        );

        send(tenant.getEmail(), subject, body);
    }

    @Async
    public void sendTicketCreatedNotification(Ticket ticket) {
        try {
            var appUser = appUserRepository.findByUsername(ticket.getRaisedBy());
            if (appUser.isEmpty() || appUser.get().getEmail() == null || appUser.get().getEmail().isBlank()) {
                log.warn("User {} has no email address, skipping ticket creation notification", ticket.getRaisedBy());
                return;
            }

            String userEmail = appUser.get().getEmail();
            String subject = String.format("Ticket #%d Created - %s", ticket.getTicketNumber(), ticket.getTitle());
            String body = String.format(
                    "Dear %s,<br><br>" +
                    "Thank you for creating a support ticket. Your ticket has been successfully registered.<br><br>" +
                    "<b>Ticket Details:</b><br>" +
                    "<b>Ticket #:</b> %d<br>" +
                    "<b>Title:</b> %s<br>" +
                    "<b>Category:</b> %s<br>" +
                    "<b>Priority:</b> %s<br>" +
                    "<b>Status:</b> %s<br>" +
                    "<b>Description:</b><br>%s<br><br>" +
                    "We will get back to you as soon as possible.<br><br>" +
                    "Kind regards,<br>" +
                    "<b>TLT Properties Support Team</b>",
                    appUser.get().getFirstName(),
                    ticket.getTicketNumber(),
                    ticket.getTitle(),
                    ticket.getCategory() != null ? ticket.getCategory().name() : "N/A",
                    ticket.getPriority() != null ? ticket.getPriority().name() : "N/A",
                    ticket.getStatus() != null ? ticket.getStatus().name() : "OPEN",
                    ticket.getDescription() != null ? ticket.getDescription() : "No description provided"
            );

            send(userEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send ticket creation notification for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    @Async
    public void sendTicketStatusUpdateNotification(Ticket ticket, String previousStatus) {
        try {
            var appUser = appUserRepository.findByUsername(ticket.getRaisedBy());
            if (appUser.isEmpty() || appUser.get().getEmail() == null || appUser.get().getEmail().isBlank()) {
                log.warn("User {} has no email address, skipping ticket status update notification", ticket.getRaisedBy());
                return;
            }

            String userEmail = appUser.get().getEmail();
            String subject = String.format("Ticket #%d Status Update - %s", ticket.getTicketNumber(), ticket.getStatus().name());
            String body = String.format(
                    "Dear %s,<br><br>" +
                    "Your support ticket has been updated.<br><br>" +
                    "<b>Ticket Details:</b><br>" +
                    "<b>Ticket #:</b> %d<br>" +
                    "<b>Title:</b> %s<br>" +
                    "<b>Previous Status:</b> %s<br>" +
                    "<b>New Status:</b> %s<br>" +
                    "<b>Category:</b> %s<br>" +
                    "<b>Priority:</b> %s<br><br>" +
                    "%s<br><br>" +
                    "If you have any questions, please reply to this email or log into your account to add comments.<br><br>" +
                    "Kind regards,<br>" +
                    "<b>TLT Properties Support Team</b>",
                    appUser.get().getFirstName(),
                    ticket.getTicketNumber(),
                    ticket.getTitle(),
                    previousStatus != null ? previousStatus : "N/A",
                    ticket.getStatus().name(),
                    ticket.getCategory() != null ? ticket.getCategory().name() : "N/A",
                    ticket.getPriority() != null ? ticket.getPriority().name() : "N/A",
                    ticket.getStatus().name().equals("CLOSED") ? 
                        "<b style='color: green;'>Your ticket has been closed. Thank you for using our support service!</b>" :
                        "We continue to work on resolving your issue."
            );

            send(userEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send ticket status update notification for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    @Async
    public void sendPaymentReceivedNotification(Tenant tenant) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            log.warn("Tenant {} {} has no email address, skipping payment received notification", tenant.getName(), tenant.getSurname());
            return;
        }

        String subject = "Payment Received - Thank You";
        String body = String.format(
                "Dear %s %s,<br><br>" +
                "<b style='color: green; font-size: 16px;'>We have received your payment. Thank you!</b><br><br>" +
                "<b>Payment Details:</b><br>" +
                "<b>Room:</b> %s (%s)<br>" +
                "<b>Amount:</b> R%.2f<br>" +
                "<b>Status:</b> PAID<br><br>" +
                "We truly appreciate your prompt payment and your valued business.<br><br>" +
                "If you have any questions about your account, please don't hesitate to contact us.<br><br>" +
                "Kind regards,<br>" +
                "<b>TLT Properties Management</b>",
                tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                tenant.getSurname(),
                tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A",
                tenant.getRoom() != null ? tenant.getRoom().getDescription() : "N/A",
                tenant.getRentalAmount() != null ? tenant.getRentalAmount().doubleValue() : 0.0
        );

        send(tenant.getEmail(), subject, body);
    }

    @Async
    public void send(String to, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", senderEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", body
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.brevo.com/v3/smtp/email",
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully to {} - Subject: {}", to, subject);
            } else {
                log.error("Failed to send email to {}: {}", to, response.getBody());
            }
        } catch (Exception e) {
            log.error("Oops! Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}