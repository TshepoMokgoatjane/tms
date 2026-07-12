package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.domain.Testimonial;

public interface TestimonialRepository extends JpaRepository<Testimonial, Integer> {
 List<Testimonial> findByActiveTrueOrderByDisplayOrderAsc();
 List<Testimonial> findAllByOrderByDisplayOrderAsc();
}
