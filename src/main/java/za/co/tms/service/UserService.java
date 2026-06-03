package za.co.tms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import za.co.tms.domain.Status;
import za.co.tms.domain.User;
import za.co.tms.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Autowired
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	public List<User> findAllUsers() {
		return userRepository.findAll();
	}
	
	public User findUserByCellPhoneNumber(String cellPhoneNumber) {
		return userRepository.findUserByCellPhoneNumber(cellPhoneNumber)
				.stream()
				.filter(user -> user.getCellPhoneNumber().equals(cellPhoneNumber))
				.findFirst()
				.orElseThrow(() -> new UsernameNotFoundException("User with phone number " + cellPhoneNumber + " not found"));
	}
	
	public User findUserByEmail(String email) {
		return userRepository.findUserByEmail(email)
				.stream()
				.filter(user -> user.getEmail().equalsIgnoreCase(email))
				.findFirst()
				.orElseThrow(() -> new UsernameNotFoundException("User with email " + email + " not found"));
	}
	
	public User findUserByUsername(String username) {
		return userRepository.findUserByUsername(username)
				.stream()
				.filter(user -> user.getUsername().equalsIgnoreCase(username))
				.findFirst()
				.orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " not found"));
	}
	
	public User findUserById(int id) {
		return userRepository.findUserById(id)
				.stream()
				.filter(user -> user.getId() == id)
				.findFirst()
				.orElseThrow(() -> new UsernameNotFoundException("User with ID " + id + " not found"));
	}
	
	public User addUser(User user) {
		user.setId(null);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return userRepository.save(user);
	}
	
	public void deleteUserById(int id) {
		User user = findUserById(id);
		user.setStatus(Status.CLOSED);
		userRepository.save(user);
	}
	
	public void updateUser(User user) {
		User existingUser = findUserById(user.getId());
		existingUser.setFirstName(user.getFirstName());
		existingUser.setLastName(user.getLastName());
		existingUser.setEmail(user.getEmail());
		existingUser.setCellPhoneNumber(user.getCellPhoneNumber());
		existingUser.setDateModified(java.time.LocalDateTime.now());
		userRepository.save(existingUser);
	}
}
