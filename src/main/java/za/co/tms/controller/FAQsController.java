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

import za.co.tms.model.FAQs;
import za.co.tms.service.FAQsService;

@RestController
@RequestMapping("/FAQs")
public class FAQsController {

	private FAQsService faqsService;
	
	@Autowired
	public FAQsController(FAQsService faqsService) {
		this.faqsService = faqsService;
	}
	
	@GetMapping(path="/find/all")
	public List<FAQs> retrieveFAQs() {
		return faqsService.findAllFAQs();
	}
	
	@GetMapping(path="/find/{id}")
	public FAQs retrieveFAQsById(@PathVariable int id) {
		return faqsService.findFAQsById(id);
	}
	
	@GetMapping(path="/find/{question}")
	public FAQs retrieveFAQsByQuestion(@PathVariable String question) {
		return faqsService.findFAQsByQuestion(question); 
	}
	
	@GetMapping(path="/find/{answer}")
	public FAQs retrieveFAQsByAnswer(@PathVariable String answer) {
		return faqsService.findFAQsByAnswer(answer);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteFAQsById(@PathVariable int id) {
		faqsService.deleteFAQsById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public FAQs updateFAQs(@PathVariable int id, @RequestBody FAQs faqs) {
		faqsService.updateFAQs(faqs);
		return faqs;
	}
	
	@PostMapping(path="/create")
	public FAQs createFAQs(@RequestBody FAQs faqs) {
		FAQs createdFAQs = faqsService.addFAQs(faqs);
		return createdFAQs;
	}
}
