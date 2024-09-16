package za.co.tms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.UserInfo;

public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
	Optional<UserInfo> findByUsername(String username);
}
