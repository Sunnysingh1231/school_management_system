package com.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import com.sms.repository.UserRepository;

@Controller
@RequestMapping("/super")
public class SuperController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	
// GET MAPPING-------------------------------------------------------------------------------------------------------------
	
	
	@GetMapping
	public String dashboard() {
		return "/super_admin_html/dashboard";
	}

}
