package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	List<User> findUserByUsername(String username);
	List<User> findUserByEmail(String email);
	List<User> findUserById(int id);
	List<User> findUserByCellPhoneNumber(String cellPhoneNumber);
}
