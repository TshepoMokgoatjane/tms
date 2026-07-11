package za.co.tms.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import za.co.tms.domain.FAQs;
import za.co.tms.service.FAQsService;

@RestController
@RequestMapping("/faqs")
@Tag(name = "FAQs Controller", description = "Endpoints for managing FAQs")
public class FAQsController {

    private final FAQsService faqsService;

    @Autowired
    public FAQsController(FAQsService faqsService) {
        this.faqsService = faqsService;
    }

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping("/active")
    @Operation(summary = "Get active FAQs", description = "Returns only active FAQs ordered by display order (public)")
    public ResponseEntity<List<FAQs>> getActiveFAQs() {
        return ResponseEntity.ok(faqsService.findActiveFAQs());
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/find/all")
    @Operation(summary = "Get all FAQs", description = "Returns all FAQs regardless of status (admin)")
    public ResponseEntity<List<FAQs>> getAllFAQs() {
        return ResponseEntity.ok(faqsService.findAllFAQs());
    }

    @GetMapping("/find/{id}")
    @Operation(summary = "Get FAQ by ID")
    public ResponseEntity<FAQs> getFAQById(@PathVariable int id) {
        return ResponseEntity.ok(faqsService.findFAQsById(id));
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new FAQ")
    public ResponseEntity<FAQs> createFAQ(@RequestBody FAQs faqs) {
        return ResponseEntity.ok(faqsService.addFAQs(faqs));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update an existing FAQ")
    public ResponseEntity<FAQs> updateFAQ(@PathVariable int id, @RequestBody FAQs faqs) {
        return ResponseEntity.ok(faqsService.updateFAQs(id, faqs));
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle FAQ active status")
    public ResponseEntity<Void> toggleActive(@PathVariable int id) {
        faqsService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a FAQ")
    public ResponseEntity<Void> deleteFAQ(@PathVariable int id) {
        faqsService.deleteFAQsById(id);
        return ResponseEntity.noContent().build();
    }
}
