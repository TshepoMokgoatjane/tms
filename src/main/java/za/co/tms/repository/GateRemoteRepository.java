package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.GateRemote;

import java.util.List;

public interface GateRemoteRepository extends JpaRepository<GateRemote, Long> {

    List<GateRemote> findByIssuedToTenantId(Integer tenantId);
}
