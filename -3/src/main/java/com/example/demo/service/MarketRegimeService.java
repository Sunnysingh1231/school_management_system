
package com.example.demo.service;

import com.example.demo.model.MarketTick;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketRegimeService {

	private static final int EMA_SHORT = 20;
	private static final int EMA_MEDIUM = 50;
	private static final int EMA_LONG = 200;

	private static final int MAX_CANDLES = 500;

	private final Map<String, StockState> stocks = new ConcurrentHashMap<>();

	private static class StockState {
		MarketTick latestTick;

		LocalDateTime currentMinute;
		double currentClose;

		final List<Double> closes = new ArrayList<>();

		Double ema20;
		Double ema50;
		Double ema200;

		String regime = "WARMING_UP";
	}

	/**
	 * Call this method for every incoming Upstox MarketTick.
	 */
	public synchronized void update(MarketTick tick) {

		if (tick == null || tick.getInstrumentKey() == null || !tick.getInstrumentKey().startsWith("NSE_EQ|")) {
			return;
		}

		// System.out.println(tick.getSymbol());

		double ltp = tick.getLtp();

		if (!Double.isFinite(ltp) || ltp <= 0) {
			return;
		}

		StockState state = stocks.computeIfAbsent(tick.getInstrumentKey(), key -> new StockState());

		state.latestTick = tick;

		LocalDateTime minute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

		if (state.currentMinute == null) {
			state.currentMinute = minute;
			state.currentClose = ltp;
			return;
		}

		if (minute.isAfter(state.currentMinute)) {

			// Previous minute candle is complete.
			state.closes.add(state.currentClose);

			if (state.closes.size() > MAX_CANDLES) {
				state.closes.remove(0);
			}

			state.currentMinute = minute;
			state.currentClose = ltp;

			calculateRegime(state);

		} else if (minute.equals(state.currentMinute)) {

			// Update current minute candle's close.
			state.currentClose = ltp;
		}
	}

	private void calculateRegime(StockState state) {

		List<Double> closes = state.closes;

		System.out.println(closes);

//        System.out.println(
//                "REGIME CALC -> candles="
//                        + closes.size()
//        );

		if (closes.size() < EMA_LONG) {

			state.ema20 = null;
			state.ema50 = null;
			state.ema200 = null;

			state.regime = "WARMING_UP";

//            System.out.println(
//                    "REGIME WARMING UP: "
//                            + closes.size()
//                            + "/200"
//            );

			return;
		}

		state.ema20 = calculateEma(closes, EMA_SHORT);

		state.ema50 = calculateEma(closes, EMA_MEDIUM);

		state.ema200 = calculateEma(closes, EMA_LONG);

		if (state.ema20 == null || state.ema50 == null || state.ema200 == null) {

			state.regime = "WARMING_UP";

			return;
		}

		if (state.ema20 > state.ema50 && state.ema50 > state.ema200) {

			state.regime = "BULLISH";

		} else if (state.ema20 < state.ema50 && state.ema50 < state.ema200) {

			state.regime = "BEARISH";

		} else {

			state.regime = "NEUTRAL";
		}

		System.out.println("REGIME RESULT = " + state.regime + " | EMA20=" + state.ema20 + " | EMA50=" + state.ema50
				+ " | EMA200=" + state.ema200);
	}

	private Double calculateEma(List<Double> closes, int period) {

		if (closes.size() < period) {
			return null;
		}

		// Seed EMA with the first period's SMA.
		double sum = 0;

		for (int i = 0; i < period; i++) {
			sum += closes.get(i);
		}

		double ema = sum / period;
		double multiplier = 2.0 / (period + 1);

		for (int i = period; i < closes.size(); i++) {
			ema = (closes.get(i) - ema) * multiplier + ema;
		}

		return ema;
	}

	/**
	 * Returns only bullish and bearish stocks. LTP is taken from the latest live
	 * MarketTick.
	 */
	public synchronized List<Map<String, Object>> getFilteredStocks() {

		List<Map<String, Object>> result = new ArrayList<>();

		for (Map.Entry<String, StockState> entry : stocks.entrySet()) {

			StockState state = entry.getValue();

			if (state.latestTick == null) {
				continue;
			}

			if (!"BULLISH".equals(state.regime) && !"BEARISH".equals(state.regime)) {
				continue;
			}

			Map<String, Object> stock = new LinkedHashMap<>();

			stock.put("instrumentKey", entry.getKey());
			stock.put("symbol", state.latestTick.getSymbol());
			stock.put("ltp", state.latestTick.getLtp());
			stock.put("change", state.latestTick.getChange());
			stock.put("changePercent", state.latestTick.getChangePercent());

			stock.put("ema20", state.ema20);
			stock.put("ema50", state.ema50);
			stock.put("ema200", state.ema200);

			stock.put("regime", state.regime);
			stock.put("timeframe", "1m");

			result.add(stock);
		}

		result.sort(Comparator.comparing(stock -> (String) stock.get("regime")));

		return result;
	}
}