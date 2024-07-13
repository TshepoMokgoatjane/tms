package za.co.tms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.model.User;
import za.co.tms.service.UserService;

@RestController
@RequestMapping(path = "/users")
public class UserController {

	private UserService userService;
	
	@Autowired
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
	@GetMapping(path = "/find/all")
	public List<User> retrieveUsers() {
		return this.userService.findAllUsers();
	}
	
	@GetMapping(path = "/find/by/{id}")
	public User retrieveUserById(@PathVariable int id) {
		return this.userService.findUserById(id);
	}
	
	@GetMapping(path = "/find/by/{email}")
	public User retrieveUserByEmail(@PathVariable String email) {
		return this.userService.findUserByEmail(email);
	}
	
	@GetMapping(path = "/find/by/cellPhoneNumber/{cellPhoneNumber}") 
	public User retrieveUserByCellPhoneNumber(@PathVariable String cellPhoneNumber) {
		return this.userService.findUserByCellPhoneNumber(cellPhoneNumber);
	}
	
	@DeleteMapping(path = "/delete/{id}")
	public ResponseEntity<Void> deleteUserById(@PathVariable int id) {
		this.userService.deleteUserById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path = "/update/{id}")
	public User updateUser(@PathVariable int id, @RequestBody User user) {
		this.userService.updateUser(user);
		return user;
	}
	
	@PostMapping(path = "/create")
	public User createUser(@RequestBody User user) {
		User createdUser = this.userService.addUser(user);
		return createdUser;
	}
}
