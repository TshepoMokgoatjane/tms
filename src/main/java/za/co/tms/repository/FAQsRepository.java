package za.co.tms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.domain.FAQCategory;
import za.co.tms.domain.FAQs;

public interface FAQsRepository extends JpaRepository<FAQs, Integer> {

    Optional<FAQs> findById(Integer id);
    List<FAQs> findByActiveTrueOrderByDisplayOrderAsc();
    List<FAQs> findByCategoryAndActiveTrueOrderByDisplayOrderAsc(FAQCategory category);
    List<FAQs> findAllByOrderByDisplayOrderAsc();
}
