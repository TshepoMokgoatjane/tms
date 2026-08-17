package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tms.domain.HouseRules;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseRulesRepository extends JpaRepository<HouseRules, Long> {

    Optional<HouseRules> findTopByActiveIsTrueOrderByVersionDesc();

    List<HouseRules> findAllByOrderByVersionDesc();

    Optional<HouseRules> findTopByOrderByVersionDesc();
}
