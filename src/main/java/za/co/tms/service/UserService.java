package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import za.co.tms.model.Status;
import za.co.tms.model.User;
import za.co.tms.repository.UserRepository;

@Service
public class UserService {

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	public List<User> findAllUsers() {
		return userRepository.findAll();
	}
	
	public User findUserByCellPhoneNumber(String cellPhoneNumber) {
		Predicate<? super User> predicate = user -> user.getCellPhoneNumber() == cellPhoneNumber;
      	return userRepository.findUserByCellPhoneNumber(cellPhoneNumber).stream().filter(predicate).findFirst().get();
	}
	
	public User findUserByEmail(String email) {
		Predicate<? super User> predicate = user -> user.getEmail().equalsIgnoreCase(email);
      	return userRepository.findUserByEmail(email).stream().filter(predicate).findFirst().get();
	}
	
	public User findUserByUsername(String username) {
		Predicate<? super User> predicate  = user -> user.getUsername().equalsIgnoreCase(username);
      	return userRepository.findUserByUsername(username).stream().filter(predicate).findFirst().get();
	}
	
	public User findUserById(int id) {
		Predicate<? super User> predicate = user -> user.getId() == id;
      	return userRepository.findUserById(id).stream().filter(predicate).findFirst().get();
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
		deleteUserById(user.getId());
		userRepository.save(user);
	}
}
