package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class DashboardController {


	@GetMapping("/")
	public String dashboard() {
		return "market";
	}

	@GetMapping("/top10")
	public String top10() {
		return "paper-trading";
	}

	@GetMapping("/algo")
	public String algo() {
		return "algo-trading";
	}

	@GetMapping("/ginn")
	public String ginn() {
		return "ginn";
	}

	@GetMapping("/test")
	public String test() {
		return "test";
	}

	@GetMapping("/chart")
	public String chart() {
		return "nifty50-chart";
	}

	
}