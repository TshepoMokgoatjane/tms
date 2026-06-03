package za.co.tms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.domain.ContactUs;

public interface ContactUsRepository extends JpaRepository<ContactUs, Integer> {
	
	Optional<ContactUs> findContactUsById(int id);
	Optional<ContactUs> findContactUsByFirstName(String firstName);
	Optional<ContactUs> findContactUsByLastName(String lastName);
	Optional<ContactUs> findContactUsByMobilePhoneNumber(String mobilePhoneNumber);
	Optional<ContactUs> findContactUsByEmailAddress(String emailAddress);
}
