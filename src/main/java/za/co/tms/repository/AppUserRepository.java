package za.co.tms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.tms.domain.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByCellPhoneNumber(String cellPhoneNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByCellPhoneNumber(String cellPhoneNumber);
}
