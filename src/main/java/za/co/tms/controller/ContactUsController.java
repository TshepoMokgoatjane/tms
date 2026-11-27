package za.co.tms.controller;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactUsController.class);

	/** Local-development only: accept the frontend's "dev-bypass" token. Off unless explicitly enabled. */
	@Value("${recaptcha.dev-bypass-enabled:false}")
	private boolean recaptchaDevBypassEnabled;
	
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
	
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
	private static final int MAX_MESSAGE_LENGTH = 5000;

	/**
	 * Server-side validation for a public submission. The form validates in the browser too, but
	 * that is trivially bypassed by calling the API directly, so the rules are enforced here.
	 * Returns a human-readable message for the first problem found, or null when the input is valid.
	 */
	private static String validateSubmission(ContactUs c) {
		if (isBlankOrShort(c.getFirstName(), 2)) {
			return "Please enter at least 2 characters for your First Name.";
		}
		if (isBlankOrShort(c.getLastName(), 2)) {
			return "Please enter at least 2 characters for your Last Name.";
		}
		String mobile = c.getMobilePhoneNumber() == null ? "" : c.getMobilePhoneNumber().trim();
		if (mobile.length() != 10) {
			return "Mobile Phone Number must be 10 digits long.";
		}
		String email = c.getEmailAddress() == null ? "" : c.getEmailAddress().trim();
		if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
			return "Please enter a valid Email Address.";
		}
		if (c.getWhereDidYouHearAboutUs() == null) {
			return "Please tell us where you heard about us.";
		}
		String message = c.getMessage() == null ? "" : c.getMessage().trim();
		if (message.isEmpty()) {
			return "Please enter a Message.";
		}
		if (message.length() > MAX_MESSAGE_LENGTH) {
			return "Your Message is too long (maximum " + MAX_MESSAGE_LENGTH + " characters).";
		}
		if (!c.isAcceptTermsAndConditions()) {
			return "You must accept the Terms and Conditions.";
		}
		return null;
	}

	private static boolean isBlankOrShort(String value, int min) {
		return value == null || value.trim().length() < min;
	}

	/** Characters outside the Basic Multilingual Plane (e.g. emoji), which need 4 bytes in UTF-8. */
	private static final Pattern FOUR_BYTE_CHARS = Pattern.compile("[\\x{10000}-\\x{10FFFF}]");

	/**
	 * The contact_us columns use MySQL's 3-byte "utf8" charset, so a 4-byte character (a phone
	 * keyboard emoji, for instance) makes the INSERT fail with "Incorrect string value". Strip
	 * those characters so the enquiry is still delivered. Remove this once the table is converted
	 * to utf8mb4.
	 */
	private static String stripFourByteChars(String value) {
		return value == null ? null : FOUR_BYTE_CHARS.matcher(value).replaceAll("");
	}

	@PostMapping(path="/create")
	public ResponseEntity<?> createContactUs(@RequestBody ContactUs contactUs, HttpServletRequest request) {
		contactUs.setFirstName(stripFourByteChars(contactUs.getFirstName()));
		contactUs.setLastName(stripFourByteChars(contactUs.getLastName()));
		contactUs.setEmailAddress(stripFourByteChars(contactUs.getEmailAddress()));
		contactUs.setMobilePhoneNumber(stripFourByteChars(contactUs.getMobilePhoneNumber()));
		contactUs.setMessage(stripFourByteChars(contactUs.getMessage()));

		String validationError = validateSubmission(contactUs);
		if (validationError != null) {
			return ResponseEntity.badRequest().body(Map.of("message", validationError));
		}

		String captchaToken = contactUs.getReCaptcha();

		// reCAPTCHA is mandatory. The only exception is the explicit local-development bypass,
		// which is disabled unless recaptcha.dev-bypass-enabled=true (never set this in production).
		boolean devBypass = recaptchaDevBypassEnabled && "dev-bypass".equals(captchaToken);
		if (!devBypass) {
			if (captchaToken == null || captchaToken.isBlank()) {
				return ResponseEntity.badRequest().body(Map.of("message", "Please complete the reCAPTCHA."));
			}
			try {
				boolean ok = recaptchaVerifierService.verify(captchaToken, request.getRemoteAddr());
				if (!ok) {
					return ResponseEntity.badRequest().body(Map.of("message", "reCAPTCHA verification failed. Please try again."));
				}
			} catch (Exception e) {
				// Fail closed: if we cannot verify the captcha we must not accept the submission.
				LOGGER.error("reCAPTCHA verification unavailable, rejecting submission: {}", e.getMessage());
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(Map.of("message", "We could not verify your request right now. Please try again shortly."));
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