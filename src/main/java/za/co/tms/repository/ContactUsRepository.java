package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.ContactUs;

public interface ContactUsRepository extends JpaRepository<ContactUs, Integer> {
	
	List<ContactUs> findContactUsById(int id);
	List<ContactUs> findContactUsByFirstName(String firstName);
	List<ContactUs> findContactUsByLastName(String lastName);
	List<ContactUs> findContactUsByMobilePhoneNumber(String mobilePhoneNumber);
	List<ContactUs> findContactUsByEmail(String email);
}
