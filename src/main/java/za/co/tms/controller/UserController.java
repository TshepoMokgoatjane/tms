package za.co.tms.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.jwt.JwtTokenRequest;
import za.co.tms.jwt.JwtTokenService;
import za.co.tms.model.User;
import za.co.tms.model.UserInfo;
import za.co.tms.service.AuthRequest;
import za.co.tms.service.UserInfoService;
import za.co.tms.service.UserService;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping(path = "/auth/user")
public class UserController {

	private UserService userService;
	private UserInfoService userInfoService;
	private JwtTokenService jwtTokenService;
	private AuthenticationManager authenticationManager;
	
	@Autowired
	public UserController(UserService userService, UserInfoService userInfoService, JwtTokenService jwtTokenService, AuthenticationManager authenticationManager) {
		this.userService = userService;
		this.userInfoService = userInfoService;
		this.jwtTokenService = jwtTokenService;
		this.authenticationManager = authenticationManager;
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
	
	@GetMapping(path = "/find/by/username/{username}")
	public User retrieveUserByUsername(@PathVariable String username) {
		return this.userService.findUserByUsername(username);
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
	
	@PostMapping("/generateToken")
	public String authenticateAndGetToken(@RequestBody JwtTokenRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));
        if (authentication.isAuthenticated()) {
            return jwtTokenService.generateToken(authRequest.username());
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
	
	 @PostMapping("/addNewUser")
    public String addNewUser(@RequestBody UserInfo userInfo) {
        return userInfoService.addUser(userInfo);
    }
}
