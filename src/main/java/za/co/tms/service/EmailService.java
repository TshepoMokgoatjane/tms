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
                        "<i>If you have already made your payment, please ignore this reminder.</i><br><br>" +
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
    public void sendImpersonationStartedNotice(String adminEmail, String adminName, String tenantName, String tenantUsername) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Admin has no email address, skipping impersonation notification");
            return;
        }

        String subject = "Security Notice: Tenant view-as session started";
        String body = String.format(
                "Dear %s,<br><br>" +
                "This is a security notification confirming that a <b>read-only \"View as Tenant\"</b> session was started from your admin account.<br><br>" +
                "<b>Viewing as:</b> %s (%s)<br>" +
                "<b>Started at:</b> %s<br>" +
                "<b>Session type:</b> Read-only (no changes can be made)<br><br>" +
                "If this was not you, please change your password immediately and contact your system administrator.<br><br>" +
                "Kind regards,<br><b>TLT Properties System</b>",
                adminName != null ? adminName : "Administrator",
                tenantName,
                tenantUsername,
                java.time.LocalDateTime.now()
        );

        send(adminEmail, subject, body);
        log.info("Impersonation start notice emailed to admin {}", adminEmail);
    }

    @Async
    public void sendOverdueRentNotice(Tenant tenant, long daysOverdue) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            log.warn("Tenant {} {} has no email address, skipping overdue rent notice", tenant.getName(), tenant.getSurname());
            return;
        }

        String subject = "URGENT: Overdue Rent Payment - Action Required";
        String body = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<div style='background: #b91c1c; padding: 20px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                "<h1 style='color: white; margin: 0; font-size: 1.5rem;'>Overdue Rent Payment</h1>" +
                "</div>" +
                "<div style='background: white; padding: 30px; border: 1px solid #e5e7eb; border-radius: 0 0 10px 10px;'>" +
                "<p>Dear %s %s,</p>" +
                "<p>Our records show that your rent payment is now <b style='color:#b91c1c;'>%d day(s) overdue</b>.</p>" +
                "<p><b>Room:</b> %s (%s)<br>" +
                "<b>Amount due:</b> R%.2f<br>" +
                "<b>Agreed payment date:</b> %s of each month</p>" +
                "<p>As per our lease agreement, rent is payable on your elected date each month. " +
                "We kindly but firmly request that you settle the outstanding amount immediately to avoid further action.</p>" +
                "<p><i>If you have already made this payment, please disregard this notice and send us proof of payment so we can update your account.</i></p>" +
                "<p>Kind regards,<br><b>TLT Properties Management</b></p>" +
                "</div>" +
                "</div>",
                tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                tenant.getSurname(),
                daysOverdue,
                tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A",
                tenant.getRoom() != null ? tenant.getRoom().getDescription() : "N/A",
                tenant.getRentalAmount() != null ? tenant.getRentalAmount().doubleValue() : 0.0,
                tenant.getPaymentDay() != null ? tenant.getPaymentDay().getLabel() : "N/A"
        );

        send(tenant.getEmail(), subject, body);
        log.info("Overdue rent notice ({} days) emailed to tenant {} {}", daysOverdue, tenant.getName(), tenant.getSurname());
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
            String subject = String.format("Ticket #%d Created - %s", ticket.getTicketNumber(), ticket.getSubject());
            String body = String.format(
                    "Dear %s,<br><br>" +
                    "Thank you for creating a support ticket. Your ticket has been successfully registered.<br><br>" +
                    "<b>Ticket Details:</b><br>" +
                    "<b>Ticket #:</b> %d<br>" +
                    "<b>Subject:</b> %s<br>" +
                    "<b>Category:</b> %s<br>" +
                    "<b>Priority:</b> %s<br>" +
                    "<b>Status:</b> %s<br>" +
                    "<b>Description:</b><br>%s<br><br>" +
                    "We will get back to you as soon as possible.<br><br>" +
                    "Kind regards,<br>" +
                    "<b>TLT Properties Support Team</b>",
                    appUser.get().getFirstName(),
                    ticket.getTicketNumber(),
                    ticket.getSubject(),
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
                    "<b>Subject:</b> %s<br>" +
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
                    ticket.getSubject(),
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
    public void sendBirthdayNotification(Tenant tenant) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            log.warn("Tenant {} {} has no email address, skipping birthday notification", tenant.getName(), tenant.getSurname());
            return;
        }

        String subject = "🎂 Happy Birthday from TLT Properties!";
        String body = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<div style='background: linear-gradient(135deg, #6b46c1, #9f67fa); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                "<h1 style='color: white; margin: 0; font-size: 2rem;'>🎂 Happy Birthday!</h1>" +
                "</div>" +
                "<div style='background: white; padding: 30px; border: 1px solid #e5e7eb; border-radius: 0 0 10px 10px;'>" +
                "<p style='font-size: 1.1rem;'>Dear %s %s,</p>" +
                "<p>Wishing you a wonderful birthday filled with joy and happiness! 🎉</p>" +
                "<p>On this special day, we want to take a moment to express our heartfelt appreciation for being such a valued tenant. It is truly a pleasure having you as part of our TLT Properties family.</p>" +
                "<p style='font-size: 1.2rem; color: #6b46c1; font-weight: bold; text-align: center;'>May this year bring you good health, happiness, and success in all that you do! 🥳</p>" +
                "<br>" +
                "<p>Warm regards,</p>" +
                "<p><b>TLT Properties Management</b></p>" +
                "</div>" +
                "</div>",
                tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                tenant.getSurname()
        );

        send(tenant.getEmail(), subject, body);
        log.info("Birthday email sent to tenant {} {}", tenant.getName(), tenant.getSurname());
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