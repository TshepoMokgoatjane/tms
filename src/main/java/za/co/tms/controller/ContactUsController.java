package za.co.tms.controller;

import java.util.List;

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

@RestController
@RequestMapping("/contact-us")
public class ContactUsController {

	private ContactUsService contactUsService;
	
	@Autowired
	public ContactUsController(ContactUsService contactUsService) {
		this.contactUsService = contactUsService;
	}
	
	@GetMapping(path="/find/all")
	public List<ContactUs> retrieveContactUs() {
		return contactUsService.findAllContactUs();
	}
	
	@GetMapping(path="/find/{id}")
	public ContactUs retrieveContactUsById(@PathVariable int id) {
		return contactUsService.findContactUsById(id);
	}
	
	@GetMapping(path="/find/{firstName}")
	public ContactUs retrieveContactUsByFirstName(@PathVariable String firstName) {
		return contactUsService.findContactUsByFirstName(firstName);
	}
	
	@GetMapping(path="/find/{lastName}")
	public ContactUs retrieveContactUsByLastName(@PathVariable String lastName) {
		return contactUsService.findContactUsByLastName(lastName);
	}
	
	@GetMapping(path="/find/{mobilePhoneNumber}")
	public ContactUs retrieveContactUsByMobilePhoneNumber(@PathVariable String mobilePhoneNumber) {
		return contactUsService.findContactUsByMobilePhoneNumber(mobilePhoneNumber);
	}
	
	@GetMapping(path="/find/{email}")
	public ContactUs retrieveContactUsByEmail(@PathVariable String email) {
		return contactUsService.findContactUsByEmail(email);
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
	public ContactUs createContactUs(@RequestBody ContactUs contactUs) {
		ContactUs createdContactUs = contactUsService.addContactUs(contactUs);
		return createdContactUs;
	}	
}