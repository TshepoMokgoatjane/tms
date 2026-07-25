package za.co.tms.service;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.tms.domain.Tenant;
import za.co.tms.domain.Ticket;
import za.co.tms.repository.AppUserRepository;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    private final AppUserRepository appUserRepository;

    @Autowired
    public SmsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio client initialized successfully");
    }

    public void sendRentReminderSms(Tenant tenant) {
        if (tenant.getCellPhoneNumber() == null || tenant.getCellPhoneNumber().isBlank()) {
            log.warn("Tenant {} {} has no phone number, skipping SMS", tenant.getName(), tenant.getSurname());
            return;
        }

        try {
            String messageBody = String.format("Hi %s %s, your rent is due today (%s). Room: %s. Thank you - TLTProperties",
                    tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                    tenant.getSurname(),
                    tenant.getPaymentDay().getLabel(),
                    tenant.getRoom() != null ? tenant.getRoom().getCode() : "N/A"
            );

            Message.creator(
                    new PhoneNumber(tenant.getCellPhoneNumber()),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("SMS sent successfully to tenant {} {}", tenant.getName(), tenant.getSurname());
        } catch (TwilioException e) {
            log.error("Failed to send SMS to tenant {} {}: {}", tenant.getName(), tenant.getSurname(), e.getMessage());
        }
    }

    public void sendSms(String mobilePhoneNumber, String smsMessage) {
        if (mobilePhoneNumber == null || mobilePhoneNumber.isBlank()) {
            log.warn("No phone number provided, skipping SMS");
            return;
        }

        try {
            // Convert SA number format (0812086672 → +27812086672)
            String formattedNumber = mobilePhoneNumber.startsWith("0")
                    ? "+27" + mobilePhoneNumber.substring(1)
                    : mobilePhoneNumber;

            Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromNumber),
                    smsMessage
            ).create();

            log.info("SMS sent successfully to {}", formattedNumber);
        } catch (TwilioException e) {
            log.error("Failed to send SMS to {}: {}", mobilePhoneNumber, e.getMessage());
        }
    }

    public void sendTicketCreatedSms(Ticket ticket) {
        try {
            var appUser = appUserRepository.findByUsername(ticket.getRaisedBy());
            if (appUser.isEmpty() || appUser.get().getCellPhoneNumber() == null || appUser.get().getCellPhoneNumber().isBlank()) {
                log.warn("User {} has no phone number, skipping ticket creation SMS", ticket.getRaisedBy());
                return;
            }

            String phoneNumber = appUser.get().getCellPhoneNumber();
            String smsMessage = String.format(
                    "Hi %s, your ticket #%d (%s) has been created successfully. We will respond shortly. Status: %s. - TLT Properties",
                    appUser.get().getFirstName(),
                    ticket.getTicketNumber(),
                    ticket.getTitle().length() > 20 ? ticket.getTitle().substring(0, 20) + "..." : ticket.getTitle(),
                    ticket.getStatus() != null ? ticket.getStatus().name() : "OPEN"
            );

            sendSms(phoneNumber, smsMessage);
        } catch (Exception e) {
            log.error("Failed to send ticket creation SMS for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }

    public void sendTicketStatusUpdateSms(Ticket ticket, String previousStatus) {
        try {
            var appUser = appUserRepository.findByUsername(ticket.getRaisedBy());
            if (appUser.isEmpty() || appUser.get().getCellPhoneNumber() == null || appUser.get().getCellPhoneNumber().isBlank()) {
                log.warn("User {} has no phone number, skipping ticket status update SMS", ticket.getRaisedBy());
                return;
            }

            String phoneNumber = appUser.get().getCellPhoneNumber();
            String statusMessage = ticket.getStatus().name().equals("CLOSED") ? 
                    "Your ticket has been CLOSED. Thank you!" : 
                    "Status updated to: " + ticket.getStatus().name();
            
            String smsMessage = String.format(
                    "Hi %s, ticket #%d has been updated. %s - TLT Properties",
                    appUser.get().getFirstName(),
                    ticket.getTicketNumber(),
                    statusMessage
            );

            sendSms(phoneNumber, smsMessage);
        } catch (Exception e) {
            log.error("Failed to send ticket status update SMS for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }
}