package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.tms.jwt.JwtTokenRequest;
import za.co.tms.jwt.JwtTokenService;
import za.co.tms.model.UserInfo;
import za.co.tms.service.UserInfoService;

@RestController
@RequestMapping(path = "/auth")
public class UserController {
	
	private UserInfoService userInfoService;
	private JwtTokenService jwtTokenService;
	private AuthenticationManager authenticationManager;
	
	@Autowired
	public UserController(UserInfoService userInfoService, JwtTokenService jwtTokenService, AuthenticationManager authenticationManager) {
		this.userInfoService = userInfoService;
		this.jwtTokenService = jwtTokenService;
		this.authenticationManager = authenticationManager;
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
	
	@GetMapping("/user/userProfile")
	@PreAuthorize("hasAuthority('ROLE_USER')")
	public String userProfile() {
		return "Welcome to User Profile";
	}
	
	@GetMapping("/admin/adminProfile") 
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public String adminProfile() {
		return "Welcome to Admin Profile";
	}
}