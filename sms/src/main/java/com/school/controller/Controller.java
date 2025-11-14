package com.school.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
@org.springframework.stereotype.Controller
public class Controller {

	@GetMapping("/")
	public String test() {
		return "index";
	}
	
}
