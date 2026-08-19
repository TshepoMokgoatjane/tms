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

    long countByLastLoginAtAfter(LocalDateTime since);

    long countByLastLoginAtIsNull();

    long countByRoleInAndStatusIs(List<UserRoles> roles, Status status);

    long countByRoleAndDateCreatedAfter(UserRoles role, LocalDateTime since);

    long countByRoleAndTenantIsNullAndStatusIs(UserRoles role, Status status);
}
