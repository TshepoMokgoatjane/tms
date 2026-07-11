package za.co.tms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.domain.FAQCategory;
import za.co.tms.domain.FAQs;
import za.co.tms.repository.FAQsRepository;

@Service
public class FAQsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FAQsService.class);

    private final FAQsRepository faqsRepository;

    @Autowired
    public FAQsService(FAQsRepository faqsRepository) {
        this.faqsRepository = faqsRepository;
    }

    public List<FAQs> findAllFAQs() {
        return faqsRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<FAQs> findActiveFAQs() {
        return faqsRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<FAQs> findActiveFAQsByCategory(FAQCategory category) {
        return faqsRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAsc(category);
    }

    public FAQs findFAQsById(int id) {
        return faqsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FAQ with ID " + id + " not found"));
    }

    public FAQs addFAQs(FAQs faqs) {
        faqs.setId(null);
        if (faqs.getCategory() == null) {
            faqs.setCategory(FAQCategory.GENERAL);
        }
        faqs.setActive(true);
        LOGGER.info("Creating FAQ: {}", faqs.getQuestion());
        return faqsRepository.save(faqs);
    }

    public FAQs updateFAQs(int id, FAQs updatedFaq) {
        FAQs existing = findFAQsById(id);
        existing.setQuestion(updatedFaq.getQuestion());
        existing.setAnswer(updatedFaq.getAnswer());
        existing.setCategory(updatedFaq.getCategory());
        existing.setDisplayOrder(updatedFaq.getDisplayOrder());
        existing.setActive(updatedFaq.isActive());
        LOGGER.info("Updated FAQ id={}", id);
        return faqsRepository.save(existing);
    }

    public void deleteFAQsById(int id) {
        LOGGER.info("Deleting FAQ id={}", id);
        faqsRepository.deleteById(id);
    }

    public void toggleActive(int id) {
        FAQs faq = findFAQsById(id);
        faq.setActive(!faq.isActive());
        faqsRepository.save(faq);
        LOGGER.info("FAQ id={} active={}", id, faq.isActive());
    }
}
