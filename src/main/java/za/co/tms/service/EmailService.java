package za.co.tms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import za.co.tms.model.Tenant;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
                    tenant.getRoomNumber().name(),
                    tenant.getRoomNumber().getRoomDescription(),
                    tenant.getRoomNumber().getMeterNumber()
            ));

            mailSender.send(message);
            log.info("Rent reminder email sent to {} ({})", tenant.getEmail(), tenant.getName());
        } catch (MailException e) {
            log.error("Failed to send rent reminder email to tenant {} {}: {}",
                    tenant.getName(), tenant.getSurname(), e.getMessage());
        }
    }
}
