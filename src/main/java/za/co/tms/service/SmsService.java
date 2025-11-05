package za.co.tms.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.tms.model.Tenant;

@Service
public class SmsService {

    @Value("${twilio.account.sid")
    private String accountSid;

    @Value("${twilio.auth.token")
    private String authToken;

    @Value("${twilio.phone.number")
    private String fromNumber;

    public void sendRentReminderSms(Tenant tenant) {
        if (tenant.getCellPhoneNumber() == null || tenant.getCellPhoneNumber().isBlank()) {
            return; // skip if no phone number
        }

        Twilio.init(accountSid, authToken);

        String messageBody = String.format("Hi %s %s, your rent is due today (%s). Room: %s. Thank you - TLTProperties",
                tenant.getTitle() != null ? tenant.getTitle().getDisplayName() : "",
                tenant.getSurname(),
                tenant.getPaymentDay().getLabel(),
                tenant.getRoomNumber().name()
        );

        Message.creator(
                new PhoneNumber(tenant.getCellPhoneNumber()),
                new PhoneNumber(fromNumber),
                messageBody
        ).create();
    }
}