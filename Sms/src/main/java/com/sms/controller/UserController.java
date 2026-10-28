package com.sms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

	@GetMapping("/")
	public String home(org.springframework.security.core.Authentication authentication) {

		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {

			if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
				return "redirect:/super";
			}

			if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SCHOOL_ADMIN"))) {
				return "redirect:/admin";
			}
		}

		return "homepage";
	}

	@GetMapping("/login")
	public String login(Authentication authentication) {

		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication.getPrincipal() instanceof String
						&& authentication.getPrincipal().equals("anonymousUser"))) {

			if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
				return "redirect:/super";
			}

			if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SCHOOL_ADMIN"))) {
				return "redirect:/admin";
			}
		}

		return "login_form";
	}

}
