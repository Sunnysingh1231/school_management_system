package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.Nifty50FiveMinuteHistoricalService;
import com.example.demo.service.TrendFilterService;
import com.example.demo.service.UpstoxMarketService;

@Controller
public class DashboardController {

	private final TrendFilterService trendFilterService;

	public DashboardController(

			TrendFilterService trendFilterService) {

		this.trendFilterService = trendFilterService;
	}

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

	@GetMapping("/trend")
	public TrendFilterService.TrendResult getTrend(@RequestParam String instrumentKey) throws Exception {

		return trendFilterService.analyzeTrend(instrumentKey);
	}
}