package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.tms.domain.CoOccupant;

import java.util.List;

@Repository
public interface CoOccupantRepository extends JpaRepository<CoOccupant, Long> {

    List<CoOccupant> findByTenantId(Integer tenantId);

    List<CoOccupant> findByTenantIdAndStatusNot(Integer tenantId, za.co.tms.domain.Status status);

    /**
     * Orphan sweep: co-occupant records that are still active (status != given) but whose main
     * tenant has already been deactivated. Used by the one-time reconcile to clean up dependents
     * left behind by deactivations that happened before the offboarding cascade existed.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT c FROM CoOccupant c WHERE c.status <> :status AND c.tenant.tenantStatus = :tenantStatus")
    List<CoOccupant> findActiveWithTenantStatus(
        @org.springframework.data.repository.query.Param("status") za.co.tms.domain.Status status,
        @org.springframework.data.repository.query.Param("tenantStatus") za.co.tms.domain.TenantStatus tenantStatus);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCellPhoneNumber(String cellPhoneNumber);
}
