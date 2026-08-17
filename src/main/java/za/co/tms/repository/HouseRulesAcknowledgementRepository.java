package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tms.domain.HouseRulesAcknowledgement;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseRulesAcknowledgementRepository extends JpaRepository<HouseRulesAcknowledgement, Long> {

    Optional<HouseRulesAcknowledgement> findByTenantIdAndHouseRulesId(Integer tenantId, Long houseRulesId);

    boolean existsByTenantIdAndHouseRulesId(Integer tenantId, Long houseRulesId);

    List<HouseRulesAcknowledgement> findByHouseRulesId(Long houseRulesId);

    List<HouseRulesAcknowledgement> findByTenantId(Integer tenantId);
}
