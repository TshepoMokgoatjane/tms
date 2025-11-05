package za.co.tms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import za.co.tms.model.Tenant;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendRentReminder(Tenant tenant) {
        if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
            return; // skip if no email
        }

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
    }
}
