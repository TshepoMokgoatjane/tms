package za.co.tms.service;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import za.co.tms.model.Status;
import za.co.tms.model.User;
import za.co.tms.repository.UserRepository;

@Service
public class UserService {

	private UserRepository userRepository;
	
	@Autowired
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	public List<User> findAllUsers() {
		return userRepository.findAll();
	}
	
	public User findUserByCellPhoneNumber(String cellPhoneNumber) {
		Predicate<? super User> predicate = user -> user.getCellPhoneNumber() == cellPhoneNumber;
		User user = userRepository.findUserByCellPhoneNumber(cellPhoneNumber).stream().filter(predicate).findFirst().get();
		return user;
	}
	
	public User findUserByEmail(String email) {
		Predicate<? super User> predicate = user -> user.getEmail().equalsIgnoreCase(email);
		User user = userRepository.findUserByEmail(email).stream().filter(predicate).findFirst().get();
		return user;
	}
	
	public User findUserById(int id) {
		Predicate<? super User> predicate = user -> user.getId() == id;
		User user = userRepository.findUserById(id).stream().filter(predicate).findFirst().get();
		return user;
	}
	
	public User addUser(User user) {
		user.setId(null);
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
