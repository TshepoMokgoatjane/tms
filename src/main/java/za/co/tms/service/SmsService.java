package za.co.tms.service;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.tms.domain.Tenant;

@Slf4j
@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

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

}