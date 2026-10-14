package za.co.tms.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import za.co.tms.domain.Ticket;
import za.co.tms.domain.TicketImage;
import za.co.tms.repository.TicketRepository;
import za.co.tms.service.EmailService;
import za.co.tms.service.SmsService;
import za.co.tms.service.TicketImageService;
import za.co.tms.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TicketController.class);

	private final TicketService ticketService;
	private final TicketRepository ticketRepository;
	private final EmailService emailService;
	private final SmsService smsService;
	private final TicketImageService ticketImageService;

	@Autowired
	public TicketController(TicketService ticketService, TicketRepository ticketRepository, EmailService emailService, SmsService smsService, TicketImageService ticketImageService) {
		this.ticketService = ticketService;
		this.ticketRepository = ticketRepository;
		this.emailService = emailService;
		this.smsService = smsService;
		this.ticketImageService = ticketImageService;
	}

	@GetMapping(path="/find/all")
	public List<Ticket> retrieveHelpdeskTickets() {
		return ticketService.findAllHelpdeskTickets();
	}

	@GetMapping(path="/find/my-tickets")
	public List<Ticket> retrieveMyTickets() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		return ticketRepository.findByRaisedBy(username);
	}

	@GetMapping(path="/find/by/{id}")
	public Ticket retrieveHelpdeskTicketById(@PathVariable Long id) {
		return ticketService.findHelpdeskTicketById(id);
	}

	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteHelpdeskTicketManagementById(@PathVariable Long id) {
		ticketService.deleteHelpdeskTicketById(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping(path="/update/{id}")
	public Ticket updateHelpdeskTicket(@PathVariable Long id, @RequestBody Ticket helpDeskTicket) {
		helpDeskTicket.setId(id);
		ticketService.updateHelpdeskTicket(helpDeskTicket);
		return helpDeskTicket;
	}

	@PostMapping("/create")
	public Ticket createHelpdeskTicketManagement(@RequestBody Ticket helpDeskTicket) {
		Ticket createdHelpdeskTicket = ticketService.addHelpdeskTicket(helpDeskTicket);
		return createdHelpdeskTicket;
	}

	// ===================== Ticket photos (optional, max 5 per ticket) =====================

	/** List photo ids attached to a ticket (for building image URLs on the client). */
	@GetMapping("/{id}/images")
	public ResponseEntity<List<Long>> getTicketImageIds(@PathVariable Long id) {
		List<Long> ids = ticketImageService.getTicketImages(id).stream()
				.map(TicketImage::getId)
				.toList();
		return ResponseEntity.ok(ids);
	}

	/** Attach a photo to a ticket. Validates type/size and the per-ticket cap in the service. */
	@PostMapping("/{id}/images")
	public ResponseEntity<?> uploadTicketImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body("Empty file.");
		}
		try {
			TicketImage saved = ticketImageService.addTicketImage(
					id, file.getContentType(), file.getOriginalFilename(), file.getSize(), file.getBytes());
			return ResponseEntity.ok(saved.getId());
		} catch (IllegalArgumentException | IllegalStateException e) {
			LOGGER.warn("Rejected ticket photo upload for ticket {}: {}", id, e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Failed to read ticket photo for ticket {}: {}", id, e.getMessage());
			return ResponseEntity.internalServerError().build();
		}
	}

	/** Serve a ticket photo's binary. */
	@GetMapping("/images/{imageId}")
	public ResponseEntity<byte[]> serveTicketImage(@PathVariable Long imageId) {
		TicketImage image = ticketImageService.getImage(imageId);
		if (image.getImageData() == null || image.getImageData().length == 0) {
			return ResponseEntity.notFound().build();
		}
		String contentType = image.getContentType() != null ? image.getContentType() : "application/octet-stream";
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
				.body(image.getImageData());
	}

	/** Delete a single ticket photo. */
	@DeleteMapping("/images/{imageId}")
	public ResponseEntity<Void> deleteTicketImage(@PathVariable Long imageId) {
		ticketImageService.deleteImage(imageId);
		return ResponseEntity.noContent().build();
	}
}