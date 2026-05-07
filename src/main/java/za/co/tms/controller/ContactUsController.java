package za.co.tms.controller;

import java.util.List;

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

import za.co.tms.model.ContactUs;
import za.co.tms.service.ContactUsService;
import za.co.tms.service.RecaptchaVerifierService;

@RestController
@RequestMapping("/contact-us")
public class ContactUsController {

	private final ContactUsService contactUsService;
	private final RecaptchaVerifierService recaptchaVerifierService;
	
	@Autowired
	public ContactUsController(ContactUsService contactUsService, RecaptchaVerifierService recaptchaVerifierService) {
		this.contactUsService = contactUsService;
		this.recaptchaVerifierService = recaptchaVerifierService;
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
		boolean ok = recaptchaVerifierService.verify(contactUs.getReCaptcha(), request.getRemoteAddr());
		if (!ok) {
			return ResponseEntity.badRequest().build();
		}
        return ResponseEntity.ok(contactUsService.addContactUs(contactUs));
	}	
}