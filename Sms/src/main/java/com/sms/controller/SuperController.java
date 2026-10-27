package com.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/super")
public class SuperController {

// GET MAPPING-------------------------------------------------------------------------------------------------------------

	@GetMapping
	public String dashboard() {
		return "/super_admin_html/dashboard";
	}

}
