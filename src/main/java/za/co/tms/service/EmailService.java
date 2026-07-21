package za.co.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import za.co.tms.domain.ContactUs;
import za.co.tms.domain.Tenant;

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