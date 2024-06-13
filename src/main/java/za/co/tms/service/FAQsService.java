package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.model.FAQs;
import za.co.tms.repository.FAQsRepository;

@Service
public class FAQsService {

	private FAQsRepository faqsRepository;
	
	@Autowired
	public FAQsService(FAQsRepository faqsRepository) {
		this.faqsRepository = faqsRepository;
	}
	
	public List<FAQs> findAllFAQs() {
		return faqsRepository.findAll();
	}
	
	public FAQs findFAQsById(int id) {
		Predicate<? super FAQs> predicate = faqs -> faqs.getId() == id;
		FAQs faqs = faqsRepository.findFAQsById(id).stream().filter(predicate).findFirst().get();
		return faqs;
	}
	
	public FAQs findFAQsByQuestion(String question) {
		Predicate<? super FAQs> predicate = faqs -> faqs.getQuestion().equalsIgnoreCase(question);
		FAQs faqs = faqsRepository.findFAQsByQuestion(question).stream().filter(predicate).findFirst().get();
		return faqs;
	}
	
	public FAQs findFAQsByAnswer(String answer) {
		Predicate<? super FAQs> predicate = faqs -> faqs.getAnswer().equalsIgnoreCase(answer);
		FAQs faqs = faqsRepository.findFAQsByAnswer(answer).stream().filter(predicate).findFirst().get();
		return faqs;
	}
	
	public FAQs addFAQs(FAQs faqs) {
		faqs.setId(null);
		return faqsRepository.save(faqs);
	}
	
	public void deleteFAQsById(int id) {
		faqsRepository.deleteById(id);
	}
	
	public void updateFAQs(FAQs faqs) {
		deleteFAQsById(faqs.getId());
		faqsRepository.save(faqs);
	}
}
