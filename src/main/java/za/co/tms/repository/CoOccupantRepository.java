package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tms.domain.CoOccupant;

import java.util.List;

@Repository
public interface CoOccupantRepository extends JpaRepository<CoOccupant, Long> {

    List<CoOccupant> findByTenantId(Integer tenantId);

    List<CoOccupant> findByTenantIdAndStatusNot(Integer tenantId, za.co.tms.domain.Status status);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCellPhoneNumber(String cellPhoneNumber);
}
