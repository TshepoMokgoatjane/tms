package za.co.tms.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.domain.Testimonial;
import za.co.tms.repository.TestimonialRepository;

@Service
public class TestimonialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestimonialService.class);

    private final TestimonialRepository testimonialRepository;

    @Autowired
    public TestimonialService(TestimonialRepository testimonialRepository) {
        this.testimonialRepository = testimonialRepository;
    }

    public List<Testimonial> findAll() {
        return testimonialRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Testimonial> findActive() {
        return testimonialRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Testimonial findById(int id) {
        return testimonialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testimonial with ID " + id + " not found"));
    }

    public Testimonial create(Testimonial testimonial) {
        testimonial.setId(null);
        testimonial.setActive(true);
        LOGGER.info("Creating testimonial from: {}", testimonial.getTenantName());
        return testimonialRepository.save(testimonial);
    }

    public Testimonial update(int id, Testimonial updated) {
        Testimonial existing = findById(id);
        existing.setTenantName(updated.getTenantName());
        existing.setRoomCode(updated.getRoomCode());
        existing.setMessage(updated.getMessage());
        existing.setRating(updated.getRating());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setActive(updated.isActive());
        LOGGER.info("Updated testimonial id={}", id);
        return testimonialRepository.save(existing);
    }

    public void toggleActive(int id) {
        Testimonial testimonial = findById(id);
        testimonial.setActive(!testimonial.isActive());
        testimonialRepository.save(testimonial);
        LOGGER.info("Testimonial id={} active={}", id, testimonial.isActive());
    }

    public void delete(int id) {
        LOGGER.info("Deleting testimonial id={}", id);
        testimonialRepository.deleteById(id);
    }
}