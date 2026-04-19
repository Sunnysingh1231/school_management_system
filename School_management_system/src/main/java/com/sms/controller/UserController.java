package com.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.sms.model.Role;
import com.sms.model.User;
import com.sms.repository.RoleRepository;
import com.sms.repository.UserRepository;
import com.sms.serviceInterface.SuperAdminServiceInterface;
import com.sms.serviceInterface.UserServiceInterface;

@Controller
public class UserController {
	
	@Autowired
	private SuperAdminServiceInterface superAdminServiceInterface;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@GetMapping("/login")
	public String login() {
		
//		superAdminServiceInterface.initializeRoles();
		
//		User user = new User();
//		user.setName("Sunny");
//		user.setEmail("123saktisingh@gmail.com");
//		user.setPassword(passwordEncoder.encode("sd"));
//		user.setPhone("8934886808");
//		
//		Role role = roleRepository.findByName("ROLE_SUPER_ADMIN");
//		
//		user.setRoles(role);
//		userRepository.save(user);
		
		return "login_form";
	}
}
