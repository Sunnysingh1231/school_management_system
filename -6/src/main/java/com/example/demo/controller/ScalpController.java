package com.example.demo.controller;

import com.example.demo.service.ScalpStrategyService;
import com.example.demo.service.ScalpStrategyService.TradeRecord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Frontend ke liye scalp data (memory se, DB nahi).
 *
 *  GET /api/scalp/trades       -> sare trades (open + closed)
 *  GET /api/scalp/open         -> sirf open trades
 *  GET /api/scalp/summary      -> win rate, total R, P&L, mode, status, interval
 *  GET /api/scalp/instruments  -> scalp me chalne wale instrument keys
 *  GET /api/scalp/candles      -> chart: candles + EMA9/21/50 + VWAP + RSI (incremental, from=N)
 */
@RestController
@RequestMapping("/api/scalp")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "scalp.enabled", havingValue = "true", matchIfMissing = false)
public class ScalpController {

	private final ScalpStrategyService service;

	public ScalpController(ScalpStrategyService service) {
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