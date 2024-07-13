package za.co.tms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.tms.model.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {
	
	List<Role> findRoleByName(String name);

}
