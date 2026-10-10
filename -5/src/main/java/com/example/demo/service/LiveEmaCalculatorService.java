package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.webSocket.EmaWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveEmaCalculatorService {

	private final EmaBuySellFilterService emaBuySellFilterService;

	private static final int EMA20 = 20;

	private static final int EMA50 = 50;

	private static final int EMA200 = 200;

	private final Map<String, List<Candle>> stockCandles = new ConcurrentHashMap<>();

	private final EmaWebSocketHandler emaWebSocketHandler;

	private final ObjectMapper objectMapper =
	        new ObjectMapper()
	                .registerModule(new JavaTimeModule());

	public LiveEmaCalculatorService(EmaWebSocketHandler emaWebSocketHandler,
			EmaBuySellFilterService emaBuySellFilterService) {

		this.emaWebSocketHandler = emaWebSocketHandler;
		this.emaBuySellFilterService = emaBuySellFilterService;
	}

	public enum TrendType {

		STRONG_BULLISH, BULLISH, SIDEWAYS, BEARISH, STRONG_BEARISH, INSUFFICIENT_DATA
	}

	// =====================================================
	// TREND RESULT
	// =====================================================

	public static class TrendResult {

		private final String instrumentKey;

		private final double currentPrice;

		private final double ema20;

		private final double ema50;

		private final double ema200;

		private final TrendType trend;

		private final double bullishStrength;

		private final double bearishStrength;

		private final boolean priceAboveEma20;

		private final boolean priceAboveEma50;

		private final boolean priceAboveEma200;

		private final boolean bullishAlignment;

		private final boolean bearishAlignment;

		private final LocalDateTime timestamp;

		public TrendResult(

				String instrumentKey,

				double currentPrice,

				double ema20,

				double ema50,

				double ema200,

				TrendType trend,

				double bullishStrength,

				double bearishStrength,

				boolean priceAboveEma20,

				boolean priceAboveEma50,

				boolean priceAboveEma200,

				boolean bullishAlignment,

				boolean bearishAlignment, LocalDateTime timestamp) {

			this.instrumentKey = instrumentKey;

			this.currentPrice = currentPrice;

			this.ema20 = ema20;

			this.ema50 = ema50;

			this.ema200 = ema200;

			this.trend = trend;

			this.bullishStrength = bullishStrength;

			this.bearishStrength = bearishStrength;

			this.priceAboveEma20 = priceAboveEma20;

			this.priceAboveEma50 = priceAboveEma50;

			this.priceAboveEma200 = priceAboveEma200;

			this.bullishAlignment = bullishAlignment;

			this.bearishAlignment = bearishAlignment;

			this.timestamp = timestamp;
		}

		public String getInstrumentKey() {
			return instrumentKey;
		}

		public double getCurrentPrice() {
			return currentPrice;
		}

		public double getEma20() {
			return ema20;
		}

		public double getEma50() {
			return ema50;
		}

		public double getEma200() {
			return ema200;
		}

		public TrendType getTrend() {
			return trend;
		}

		public double getBullishStrength() {
			return bullishStrength;
		}

		public double getBearishStrength() {
			return bearishStrength;
		}

		public boolean isPriceAboveEma20() {
			return priceAboveEma20;
		}

		public boolean isPriceAboveEma50() {
			return priceAboveEma50;
		}

		public boolean isPriceAboveEma200() {
			return priceAboveEma200;
		}

		public boolean isBullishAlignment() {
			return bullishAlignment;
		}

		public boolean isBearishAlignment() {
			return bearishAlignment;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}
	}

	public void addCandle(Candle candle) {

		String instrumentKey = candle.getInstrumentKey();

		List<Candle> candles = stockCandles.computeIfAbsent(instrumentKey, key -> new ArrayList<>());

		synchronized (candles) {

			candles.add(candle);

			if (candles.size() > 500) {

				candles.remove(0);
			}

			if (candles.size() >= EMA200) {
				
				System.out.println(candles.size());

				analyzeTrend(instrumentKey, candles);
			}
		}
	}

	private void analyzeTrend(String instrumentKey, List<Candle> candles) {

		List<Double> closes = new ArrayList<>();

		for (Candle candle : candles) {

			closes.add(candle.getClose());
			
		}

		double ema20 = calculateEMA(closes, EMA20);

		double ema50 = calculateEMA(closes, EMA50);

		double ema200 = calculateEMA(closes, EMA200);

		double currentPrice = closes.get(closes.size() - 1);

		LocalDateTime timestamp = candles.get(candles.size() - 1).getTimestamp();

		boolean above20 = currentPrice > ema20;

		boolean above50 = currentPrice > ema50;

		boolean above200 = currentPrice > ema200;

		boolean bullishAlignment = ema20 > ema50 && ema50 > ema200;

		boolean bearishAlignment = ema20 < ema50 && ema50 < ema200;

		double bullishStrength = 0;

		double bearishStrength = 0;

		if (above20) {

			bullishStrength += 20;
		}

		if (above50) {

			bullishStrength += 20;
		}

		if (above200) {

			bullishStrength += 20;
		}

		if (bullishAlignment) {

			bullishStrength += 40;
		}

		if (!above20) {

			bearishStrength += 20;
		}

		if (!above50) {

			bearishStrength += 20;
		}

		if (!above200) {

			bearishStrength += 20;
		}

		if (bearishAlignment) {

			bearishStrength += 40;
		}

		TrendType trend;

		if (bullishStrength >= 80) {

			trend = TrendType.STRONG_BULLISH;

		} else if (bullishStrength >= 60) {

			trend = TrendType.BULLISH;

		} else if (bearishStrength >= 80) {

			trend = TrendType.STRONG_BEARISH;

		} else if (bearishStrength >= 60) {

			trend = TrendType.BEARISH;

		} else {

			trend = TrendType.SIDEWAYS;
		}

		TrendResult result =

				new TrendResult(

						instrumentKey,

						currentPrice,

						ema20,

						ema50,

						ema200,

						trend,

						bullishStrength,

						bearishStrength,

						above20,

						above50,

						above200,

						bullishAlignment,

						bearishAlignment,
						
						timestamp);

		emaBuySellFilterService.processAllStocks(result);

		try {

			String json = objectMapper.writeValueAsString(result);

			emaWebSocketHandler.sendMessage(json);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private double calculateEMA(List<Double> prices, int period) {

		if (prices.size() < period) {

			return 0;
		}

		// Initial SMA

		double sum = 0;

		for (int i = 0; i < period; i++) {

			sum += prices.get(i);
		}

		double ema = sum / period;

		// EMA multiplier

		double multiplier = 2.0 / (period + 1);

		// Remaining candles

		for (int i = period; i < prices.size(); i++) {

			double price = prices.get(i);

			ema = (price - ema) * multiplier + ema;
		}

		return ema;
	}

}