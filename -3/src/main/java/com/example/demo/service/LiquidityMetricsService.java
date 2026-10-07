package com.example.demo.service;

import com.example.demo.model.MarketTick;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiquidityMetricsService {

	private static final double TRADED_VALUE_THRESHOLD = 50_00_00_000.0;

	private static final double SPREAD_PERCENT_THRESHOLD = 0.05;

	private final Map<String, LiquidityState> stocks = new ConcurrentHashMap<>();

	private static class LiquidityState {

		MarketTick latestTick;

		double tradedValue;

		double bestBid;

		double bestAsk;

		double spread;

		double spreadPercent;

		boolean tradedValueSafe;

		boolean spreadSafe;
	}

	public void update(MarketTick tick) {

		if (tick == null || tick.getInstrumentKey() == null || !tick.getInstrumentKey().startsWith("NSE_EQ|")) {

			return;
		}
		// System.out.println(tick.getSymbol());

		double ltp = tick.getLtp();

		if (!Double.isFinite(ltp) || ltp <= 0) {
			return;
		}

		LiquidityState state = stocks.computeIfAbsent(tick.getInstrumentKey(), key -> new LiquidityState());

		state.latestTick = tick;

		double volume = tick.getVolume();

		if (Double.isFinite(volume) && volume >= 0) {

			state.tradedValue = volume * ltp;

			state.tradedValueSafe = state.tradedValue >= TRADED_VALUE_THRESHOLD;

		} else {

			state.tradedValue = 0;

			state.tradedValueSafe = false;
		}

		state.bestBid = getBestBid(tick);

		state.bestAsk = getBestAsk(tick);

		/*
		 * ===================================================== 3. BID-ASK SPREAD
		 * =====================================================
		 */

		if (state.bestBid > 0 && state.bestAsk > 0 && state.bestAsk >= state.bestBid) {

			state.spread = Math.abs(state.bestAsk - state.bestBid);

			/*
			 * Spread %
			 *
			 * (Spread / LTP) × 100
			 */
			state.spreadPercent = (state.spread / ltp) * 100.0;

			/*
			 * Safe if spread < 0.05%
			 */
			state.spreadSafe = state.spreadPercent < SPREAD_PERCENT_THRESHOLD;

		} else {

			state.spread = 0;

			state.spreadPercent = 0;

			state.spreadSafe = false;
		}
	}

	/*
	 * Get first/best bid price.
	 */
	private double getBestBid(MarketTick tick) {

		if (tick.getBidLevels() == null || tick.getBidLevels().isEmpty()) {

			return 0;
		}

		var firstBid = tick.getBidLevels().get(0);

		if (firstBid == null) {
			return 0;
		}

		double price = firstBid.getPrice();

		if (!Double.isFinite(price) || price <= 0) {
			return 0;
		}

		return price;
	}

	/*
	 * Get first/best ask price.
	 */
	private double getBestAsk(MarketTick tick) {

		if (tick.getAskLevels() == null || tick.getAskLevels().isEmpty()) {

			return 0;
		}

		var firstAsk = tick.getAskLevels().get(0);

		if (firstAsk == null) {
			return 0;
		}

		double price = firstAsk.getPrice();

		if (!Double.isFinite(price) || price <= 0) {
			return 0;
		}

		return price;
	}

	/**
	 * Returns all stocks with calculated liquidity metrics.
	 */
	public synchronized List<Map<String, Object>> getLiquidityStocks() {

		List<Map<String, Object>> result = new ArrayList<>();

		for (Map.Entry<String, LiquidityState> entry : stocks.entrySet()) {

			LiquidityState state = entry.getValue();

			if (state.latestTick == null) {
				continue;
			}

			// Sirf tradedValueSafe == true wale stocks
			if (!state.tradedValueSafe) {
				continue;
			}

//            if (!state.spreadSafe) {
//                continue;
//            }

			MarketTick tick = state.latestTick;

			Map<String, Object> stock = new LinkedHashMap<>();

			if (tick.getVolume() <= 1000000) {
				continue;
			}

			if (tick.getChangePercent() >= 1.5) {
				continue;
			}

			// Basic market data
			stock.put("instrumentKey", entry.getKey());
			stock.put("symbol", tick.getSymbol());
			stock.put("ltp", tick.getLtp());
			stock.put("changePercent", tick.getChangePercent());
			stock.put("volume", tick.getVolume());

			// Today's Traded Value
			stock.put("tradedValue", state.tradedValue);
			stock.put("tradedValueCrore", state.tradedValue / 1_00_00_000.0);
			stock.put("tradedValueSafe", state.tradedValueSafe);

			// Bid / Ask
			stock.put("bestBid", state.bestBid);
			stock.put("bestAsk", state.bestAsk);

			// Spread
			stock.put("spread", state.spread);
			stock.put("spreadPercent", state.spreadPercent);
			stock.put("spreadSafe", state.spreadSafe);

			// Overall liquidity condition
			stock.put("liquiditySafe", state.tradedValueSafe && state.spreadSafe);

			result.add(stock);
		}

		return result;
	}
}