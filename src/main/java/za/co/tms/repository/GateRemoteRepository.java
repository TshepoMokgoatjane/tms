package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.GateRemote;

import java.util.List;
import java.util.Optional;

public interface GateRemoteRepository extends JpaRepository<GateRemote, Long> {

    Optional<GateRemote> findByIssuedToTenantId(Integer tenantId);

    List<GateRemote> findByRoomId(Long roomId);
}
