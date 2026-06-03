package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.domain.FAQs;

public interface FAQsRepository extends JpaRepository<FAQs, Integer> {

	List<FAQs> findFAQsById(int id);
	List<FAQs> findFAQsByQuestion(String question);
	List<FAQs> findFAQsByAnswer(String answer);
}
