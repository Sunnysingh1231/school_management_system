package com.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

	@GetMapping("/")
	public String home(org.springframework.security.core.Authentication authentication) {

		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return "redirect:/dashboard";
		}

		return "index";
	}

	@GetMapping("/login")
	public String login() {
		return "login_form";
	}
}
