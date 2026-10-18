package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.AppUser;
import za.co.tms.domain.Status;
import za.co.tms.domain.UserRoles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByCellPhoneNumber(String cellPhoneNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCellPhoneNumber(String cellPhoneNumber);

    List<AppUser> findByRoleAndTenantIsNull(UserRoles role);

    List<AppUser> findByTenantIdAndRole(Integer tenantId, UserRoles role);

    /**
     * Orphan sweep: co-occupant login accounts that are still active (status != given) but whose
     * linked tenant has already been deactivated. Used by the one-time reconcile.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM AppUser u WHERE u.role = :role AND u.status <> :status AND u.tenant.tenantStatus = :tenantStatus")
    List<AppUser> findActiveByRoleWithTenantStatus(
        @org.springframework.data.repository.query.Param("role") UserRoles role,
        @org.springframework.data.repository.query.Param("status") Status status,
        @org.springframework.data.repository.query.Param("tenantStatus") za.co.tms.domain.TenantStatus tenantStatus);

    long countByLastLoginAtAfter(LocalDateTime since);

    long countByLastLoginAtIsNull();

    long countByRoleInAndStatusIs(List<UserRoles> roles, Status status);

    long countByRoleAndDateCreatedAfter(UserRoles role, LocalDateTime since);

    long countByRoleAndTenantIsNullAndStatusIs(UserRoles role, Status status);
}
