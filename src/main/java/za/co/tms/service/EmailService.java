package za.co.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import za.co.tms.domain.ContactUs;
import za.co.tms.domain.Tenant;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${contact.notification.email}")
    private String contactNotificationEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactUsNotification(ContactUs contactUs) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(contactNotificationEmail);
            message.setSubject("New Contact Us Submission from " + contactUs.getFirstName() + " " + contactUs.getLastName());
            message.setText(String.format(
                    "You have received a new Contact Us submission:\n\n" +
                    "Name: %s %s\n" +
                    "Email: %s\n" +
                    "Phone: %s\n" +
                    "Heard about us via: %s\n\n" +
                    "Message:\n%s\n",
                    contactUs.getFirstName(),
                    contactUs.getLastName(),
                    contactUs.getEmailAddress() != null ? contactUs.getEmailAddress() : "Not provided",
                    contactUs.getMobilePhoneNumber() != null ? contactUs.getMobilePhoneNumber() : "Not provided",
                    contactUs.getWhereDidYouHearAboutUs() != null ? contactUs.getWhereDidYouHearAboutUs().name() : "Not specified",
                    contactUs.getMessage() != null ? contactUs.getMessage() : "No message"
            ));

            if (contactUs.getEmailAddress() != null && !contactUs.getEmailAddress().isBlank()) {
                message.setReplyTo(contactUs.getEmailAddress());
            }

            mailSender.send(message);
            log.info("Contact Us notification sent to {} for submission from {} {}",
                    contactNotificationEmail, contactUs.getFirstName(), contactUs.getLastName());
        } catch (MailException e) {
            log.error("Failed to send Contact Us notification for {} {}: {}",
                    contactUs.getFirstName(), contactUs.getLastName(), e.getMessage());
        }
    }

    public void sendRentReminder(Tenant tenant) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            log.warn("Tenant {} {} has no email address, skipping email", tenant.getName(), tenant.getSurname());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tenant.getEmail());
            message.setSubject("Rent Payment Reminder");
            message.setText(String.format(
                    "Dear %s %s, \n\nThis is a friendly reminder that your rent is due today (%s).\n" +
                            "Room: %s (%s)\nMeter Number: %s\n\nThank you,\nTMS Management",
                    tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                    tenant.getSurname(),
                    tenant.getPaymentDay().getLabel(),
                    tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A",
                    tenant.getRoom() != null ? tenant.getRoom().getDescription() : "N/A",
                    tenant.getRoom() != null ? tenant.getRoom().getPrepaidElectricityMeterNumber() : "N/A"
            ));

            mailSender.send(message);
            log.info("Rent reminder email sent to {} ({})", tenant.getEmail(), tenant.getName());
        } catch (MailException e) {
            log.error("Failed to send rent reminder email to tenant {} {}: {}",
                    tenant.getName(), tenant.getSurname(), e.getMessage());
        }
    }
}
