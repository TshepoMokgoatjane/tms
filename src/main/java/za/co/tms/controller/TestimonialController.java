package za.co.tms.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import za.co.tms.domain.Testimonial;
import za.co.tms.service.TestimonialService;

@RestController
@RequestMapping("/api/testimonials")
@Tag(name = "Testimonial Controller", description = "Endpoints for managing testimonials")
public class TestimonialController {

 private final TestimonialService testimonialService;

 @Autowired
 public TestimonialController(TestimonialService testimonialService) {
 this.testimonialService = testimonialService;
 }

 // ========== PUBLIC ENDPOINTS ==========

 @GetMapping("/active")
 @Operation(summary = "Get active testimonials", description = "Returns only active testimonials for the public page")
 public ResponseEntity<List<Testimonial>> getActiveTestimonials() {
 return ResponseEntity.ok(testimonialService.findActive());
 }

 // ========== ADMIN ENDPOINTS ==========

 @GetMapping("/all")
 @Operation(summary = "Get all testimonials", description = "Returns all testimonials regardless of status")
 public ResponseEntity<List<Testimonial>> getAllTestimonials() {
 return ResponseEntity.ok(testimonialService.findAll());
 }

 @GetMapping("/{id}")
 @Operation(summary = "Get testimonial by ID")
 public ResponseEntity<Testimonial> getById(@PathVariable int id) {
 return ResponseEntity.ok(testimonialService.findById(id));
 }

 @PostMapping
 @Operation(summary = "Create a new testimonial")
 public ResponseEntity<Testimonial> create(@RequestBody Testimonial testimonial) {
 return ResponseEntity.ok(testimonialService.create(testimonial));
 }

 @PutMapping("/{id}")
 @Operation(summary = "Update a testimonial")
 public ResponseEntity<Testimonial> update(@PathVariable int id, @RequestBody Testimonial testimonial) {
 return ResponseEntity.ok(testimonialService.update(id, testimonial));
 }

 @PutMapping("/{id}/toggle-active")
 @Operation(summary = "Toggle testimonial visibility")
 public ResponseEntity<Void> toggleActive(@PathVariable int id) {
 testimonialService.toggleActive(id);
 return ResponseEntity.ok().build();
 }

 @DeleteMapping("/{id}")
 @Operation(summary = "Delete a testimonial")
 public ResponseEntity<Void> delete(@PathVariable int id) {
 testimonialService.delete(id);
 return ResponseEntity.noContent().build();
 }
}
