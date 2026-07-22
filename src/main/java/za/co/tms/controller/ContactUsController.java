package za.co.tms.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.domain.ContactUs;
import za.co.tms.service.ContactUsService;
import za.co.tms.service.EmailService;
import za.co.tms.service.RecaptchaVerifierService;
import za.co.tms.service.SmsService;

@RestController
@RequestMapping("/contact-us")
public class ContactUsController {

	private final ContactUsService contactUsService;
	private final RecaptchaVerifierService recaptchaVerifierService;
	private final SmsService smsService;
	private final EmailService emailService;
	
	@Autowired
	public ContactUsController(ContactUsService contactUsService, RecaptchaVerifierService recaptchaVerifierService, SmsService smsService, EmailService emailService) {
		this.contactUsService = contactUsService;
		this.recaptchaVerifierService = recaptchaVerifierService;
		this.smsService = smsService;
		this.emailService = emailService;
	}
	
	@GetMapping(path="/find/all")
	public List<ContactUs> retrieveContactUs() {
		return contactUsService.findAllContactUs();
	}
	
	@GetMapping(path="/find/by-id/{id}")
	public ContactUs retrieveContactUsById(@PathVariable int id) {
		return contactUsService.findContactUsById(id);
	}
	
	@GetMapping(path="/find/by-first-name/{firstName}")
	public ContactUs retrieveContactUsByFirstName(@PathVariable String firstName) {
		return contactUsService.findContactUsByFirstName(firstName);
	}
	
	@GetMapping(path="/find/by-last-name/{lastName}")
	public ContactUs retrieveContactUsByLastName(@PathVariable String lastName) {
		return contactUsService.findContactUsByLastName(lastName);
	}
	
	@GetMapping(path="/find/by-mobile/{mobilePhoneNumber}")
	public ContactUs retrieveContactUsByMobilePhoneNumber(@PathVariable String mobilePhoneNumber) {
		return contactUsService.findContactUsByMobilePhoneNumber(mobilePhoneNumber);
	}
	
	@GetMapping(path="/find/by-email-address/{emailAddress}")
	public ContactUs retrieveContactUsByEmailAddress(@PathVariable String emailAddress) {
		return contactUsService.findContactUsByEmailAddress(emailAddress);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteContactUsById(@PathVariable int id) {
		contactUsService.deleteContactUsById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public ContactUs updateContactUs(@PathVariable int id, @RequestBody ContactUs contactUs) {
		contactUsService.updateContactUs(contactUs);
		return contactUs;
	}
	
	@PostMapping(path="/create")
	public ResponseEntity<ContactUs> createContactUs(@RequestBody ContactUs contactUs, HttpServletRequest request) {
		String captchaToken = contactUs.getReCaptcha();

		// Skip reCAPTCHA verification for development bypass token or empty tokens
		if (captchaToken != null && !captchaToken.isBlank() && !"dev-bypass".equals(captchaToken)) {
			try {
				boolean ok = recaptchaVerifierService.verify(captchaToken, request.getRemoteAddr());
				if (!ok) {
					return ResponseEntity.badRequest().build();
				}
			} catch (Exception e) {
				// If reCAPTCHA verification fails due to network issues, log and continue
				// In production, you may want to reject the request instead
				System.out.println("reCAPTCHA verification failed (network issue), proceeding anyway: " + e.getMessage());
			}
		}

		return ResponseEntity.ok(contactUsService.addContactUs(contactUs));
	}	

	@PutMapping(path="/reply/{id}")
public ResponseEntity<ContactUs> replyToContactUs(@PathVariable int id, @RequestBody Map<String, String> body) {
    ContactUs contactUs = contactUsService.findContactUsById(id);

    String replyMessage = body.get("resolution");
    contactUs.setResolution(replyMessage);
    contactUsService.updateContactUs(contactUs);

    // Send email reply to the user
    if (contactUs.getEmailAddress() != null && !contactUs.getEmailAddress().isBlank()) {
        String subject = "RE: Your enquiry to TLT Properties";
        String emailBody = String.format(
                "Dear %s %s,<br><br>" +
                "Thank you for reaching out to us. Here is our response to your enquiry:<br><br>" +
                "<blockquote style=\"border-left: 3px solid #4b0082; padding-left: 10px; color: #333;\">%s</blockquote><br>" +
                "If you have further questions, please don't hesitate to contact us.<br><br>" +
                "Kind regards,<br>" +
                "<b>TLT Properties</b><br>" +
                "Phone: (+27) 81 208 6672<br>" +
                "Website: <a href=\"https://www.tltproperties.co.za\">www.tltproperties.co.za</a>",
                contactUs.getFirstName(),
                contactUs.getLastName(),
                replyMessage
        );
        emailService.send(contactUs.getEmailAddress(), subject, emailBody);
    }

    // Send SMS reply to the user
    if (contactUs.getMobilePhoneNumber() != null && !contactUs.getMobilePhoneNumber().isBlank()) {
        String smsMessage = String.format(
                "Hi %s, TLT Properties has responded to your enquiry: %s. Visit www.tltproperties.co.za for more info.",
                contactUs.getFirstName(),
                replyMessage.length() > 100 ? replyMessage.substring(0, 100) + "..." : replyMessage
        );
        smsService.sendSms(contactUs.getMobilePhoneNumber(), smsMessage);
    }

    return ResponseEntity.ok(contactUs);
}

}