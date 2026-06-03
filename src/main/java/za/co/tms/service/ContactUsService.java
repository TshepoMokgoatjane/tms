package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.domain.ContactUs;
import za.co.tms.repository.ContactUsRepository;

@Service
public class ContactUsService {
	
	private final ContactUsRepository contactUsRepository;
	
	@Autowired
	public ContactUsService(ContactUsRepository contactUsRepository) {
		this.contactUsRepository = contactUsRepository;
	}
	
	public List<ContactUs> findAllContactUs() {
		return contactUsRepository.findAll();
	}
	
	public ContactUs findContactUsById(int id) {
		Predicate<? super ContactUs> predicate = contactUs -> contactUs.getId() == id;
        return contactUsRepository.findContactUsById(id).stream().filter(predicate).findFirst().get();
	}
	
	public ContactUs findContactUsByFirstName(String firstName) {
		Predicate<? super ContactUs> predicate = contactUs -> contactUs.getFirstName().equalsIgnoreCase(firstName);
        return contactUsRepository.findContactUsByFirstName(firstName).stream().filter(predicate).findFirst().get();
	}
	
	public ContactUs findContactUsByLastName(String lastName) {
		Predicate<? super ContactUs> predicate = contactUs -> contactUs.getLastName().equalsIgnoreCase(lastName);
        return contactUsRepository.findContactUsByLastName(lastName).stream().filter(predicate).findFirst().get();
	}
	
	public ContactUs findContactUsByEmailAddress(String emailAddress) {
		Predicate<? super ContactUs> predicate = contactUs -> contactUs.getEmailAddress().equalsIgnoreCase(emailAddress);
        return contactUsRepository.findContactUsByEmailAddress(emailAddress).stream().filter(predicate).findFirst().get();
	}
	
	public ContactUs findContactUsByMobilePhoneNumber(String mobilePhoneNumber) {
		Predicate<? super ContactUs> predicate = contactUs -> contactUs.getMobilePhoneNumber().equalsIgnoreCase(mobilePhoneNumber);
        return contactUsRepository.findContactUsByMobilePhoneNumber(mobilePhoneNumber).stream().filter(predicate).findFirst().get();
	}
	
	public ContactUs addContactUs(ContactUs contactUs) {
		contactUs.setId(null);
		
		return contactUsRepository.save(contactUs);
	}
	
	public void deleteContactUsById(int id) {
		contactUsRepository.deleteById(id);
	}
	
	public void updateContactUs(ContactUs contactUs) {
		deleteContactUsById(contactUs.getId());
		contactUsRepository.save(contactUs);
	}

}