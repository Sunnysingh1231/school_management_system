package com.example.demo.controller;

import com.example.demo.service.EmaVwapStrategyService;
import com.example.demo.service.EmaVwapStrategyService.TradeRecord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * EMA + VWAP strategy ka data frontend ke liye (memory se, DB nahi).
 *
 *  GET /api/emavwap/trades       -> sare trades (open + closed)
 *  GET /api/emavwap/open         -> sirf open trades
 *  GET /api/emavwap/summary      -> win rate, total R, P&L, mode, status, interval
 *  GET /api/emavwap/instruments  -> chalne wale instrument keys
 *  GET /api/emavwap/candles      -> chart: candles + EMA9/20/50 + VWAP + signal (incremental, from=N)
 */
@RestController
@RequestMapping("/api/emavwap")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "emavwap.enabled", havingValue = "true", matchIfMissing = false)
public class EmaVwapController {

	private final EmaVwapStrategyService service;

	public EmaVwapController(EmaVwapStrategyService service) {
		this.service = service;
	}

	@GetMapping("/trades")
	public List<TradeRecord> trades() {
		return service.getTrades();
	}

	@GetMapping("/open")
	public List<TradeRecord> open() {
		return service.getOpenTrades();
	}

	@GetMapping("/summary")
	public Map<String, Object> summary() {
		return service.getSummary();
	}

	@GetMapping("/instruments")
	public List<String> instruments() {
		return service.getInstrumentKeys();
	}

	@GetMapping("/candles")
	public Map<String, Object> candles(@RequestParam("instrumentKey") String instrumentKey,
			@RequestParam(value = "from", defaultValue = "0") int from) {

		return service.getSeries(instrumentKey, from);
	}
}