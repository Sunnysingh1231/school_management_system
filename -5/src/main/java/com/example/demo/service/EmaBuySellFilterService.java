package com.example.demo.service;

import com.example.demo.model.EmaTrade;
import com.example.demo.repository.EmaTradeRepository;
import com.example.demo.service.LiveEmaCalculatorService.TrendResult;
import com.example.demo.webSocket.EmaBuySellHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmaBuySellFilterService {

	private final EmaBuySellHandler emaBuySellHandler;

	private final EmaTradeRepository emaTradeRepository;

	// =========================================================
	// CONFIG (application.properties se override kar sakte ho)
	// =========================================================

	/** Stop-loss entry price ka kitna % door (1R = ye distance) */
	@Value("${trade.stoploss-percent:0.5}")
	private double stopLossPercent;

	/** Risk:Reward ratio. 2.0 => target = 2R (1:2) */
	@Value("${trade.risk-reward:2.0}")
	private double riskReward;

	/** Kitne R profit par trailing start ho (1.0 => 1R profit par; SL breakeven tak aa jata hai) */
	@Value("${trade.trail-activation-r:1.0}")
	private double trailActivationR;

	/** Trailing SL best price se kitna peeche rahe (R me). 1.0 => 1R peeche */
	@Value("${trade.trail-distance-r:1.0}")
	private double trailDistanceR;

	/** true => target (RR) hit hone par exit. false => sirf trailing SL se exit (profit run karne do) */
	@Value("${trade.use-fixed-target:true}")
	private boolean useFixedTarget;

	/** Entry ke liye strength threshold (original code jaisa 90) */
	@Value("${trade.entry-strength:90}")
	private double entryStrength;

	/**
	 * Signal exit threshold. Original code me 90 tha (strength 90 se niche aate hi exit).
	 * Dhyan do: entry 90 par aur exit bhi 90 par rakhoge to signal exit SL/trailing se pehle
	 * hi trade band kar dega. SL/trailing/target ko kaam karne dene ke liye isse kam rakho
	 * (jaise 50) ya 0 karke signal exit band kar do.
	 */
	@Value("${trade.exit-strength:90}")
	private double exitStrength;

	/** Sirf in instruments par trade hoga (original me hardcoded tha) */
	private static final Set<String> ALLOWED_INSTRUMENTS = Set.of("NSE_EQ|INE090A01021");

	// =========================================================
	// STATE
	// =========================================================

	/** instrumentKey -> open position (ek instrument par ek hi position) */
	private final Map<String, Position> openPositions = new ConcurrentHashMap<>();

	/**
	 * SL/target/trailing se exit ke baad, jab tak strength entry level se niche
	 * reset na ho, wahi side dobara enter nahi hogi (warna SL hit hote hi turant re-entry ho jati).
	 * Format: "instrumentKey|BUY" ya "instrumentKey|SELL"
	 */
	private final Set<String> reentryBlock = new HashSet<>();

	public EmaBuySellFilterService(EmaBuySellHandler emaBuySellHandler, EmaTradeRepository emaTradeRepository) {

		this.emaBuySellHandler = emaBuySellHandler;
		this.emaTradeRepository = emaTradeRepository;
	}

	// =========================================================
	// POSITION HOLDER (trailing state yahan, entity me change nahi)
	// =========================================================

	private static class Position {

		final EmaTrade trade;
		final boolean isLong;
		final double entryPrice;
		final double riskPerUnit; // 1R in price terms
		final double target;

		double stopLoss;
		double extremePrice; // long me highest, short me lowest price
		boolean trailing;

		Position(EmaTrade trade, boolean isLong, double entryPrice, double riskPerUnit, double stopLoss,
				double target) {

			this.trade = trade;
			this.isLong = isLong;
			this.entryPrice = entryPrice;
			this.riskPerUnit = riskPerUnit;
			this.stopLoss = stopLoss;
			this.target = target;
			this.extremePrice = entryPrice;
		}
	}

	// =========================================================
	// MAIN ENTRY POINT (har tick/update par call hota hai)
	// =========================================================

	public synchronized void processAllStocks(TrendResult tick) {

		String instrumentKey = tick.getInstrumentKey();

		if (!ALLOWED_INSTRUMENTS.contains(instrumentKey)) {
			return;
		}

		double price = tick.getCurrentPrice();
		double bullish = tick.getBullishStrength();
		double bearish = tick.getBearishStrength();

		// Strength reset ho gayi to us side ka re-entry block hata do
		if (bullish < entryStrength) {
			reentryBlock.remove(instrumentKey + "|BUY");
		}

		if (bearish < entryStrength) {
			reentryBlock.remove(instrumentKey + "|SELL");
		}

		// -----------------------------------------------------
		// 1) OPEN POSITION HAI -> pehle SL / trailing / target / signal check
		// -----------------------------------------------------

		Position position = openPositions.get(instrumentKey);

		if (position != null) {

			managePosition(position, tick, price, bullish, bearish);

			// Exit ke same tick par naya entry nahi
			return;
		}

		// -----------------------------------------------------
		// 2) POSITION NAHI HAI -> entry check
		// -----------------------------------------------------

		if (bullish > entryStrength && !reentryBlock.contains(instrumentKey + "|BUY")) {

			openPosition(tick, true, price);

		} else if (bearish > entryStrength && !reentryBlock.contains(instrumentKey + "|SELL")) {

			openPosition(tick, false, price);
		}
	}

	// =========================================================
	// OPEN POSITION (SL + TARGET set)
	// =========================================================

	private void openPosition(TrendResult tick, boolean isLong, double price) {

		if (price <= 0) {
			return;
		}

		double risk = price * stopLossPercent / 100.0;

		double stopLoss = isLong ? price - risk : price + risk;

		double target = isLong ? price + risk * riskReward : price - risk * riskReward;

		EmaTrade emaTrade = new EmaTrade();

		emaTrade.setInstrumentKey(tick.getInstrumentKey());
		emaTrade.setTrend(String.valueOf(tick.getTrend()));

		if (isLong) {

			emaTrade.setStatus("BUY");
			emaTrade.setBuyPrice(price);
			emaTrade.setBuyTime(tick.getTimestamp());

		} else {

			emaTrade.setStatus("SELL");
			emaTrade.setSellPrice(price);
			emaTrade.setSellTime(tick.getTimestamp());
		}

		openPositions.put(tick.getInstrumentKey(), new Position(emaTrade, isLong, price, risk, stopLoss, target));

		System.out.printf("OPEN %s | %s | %s | entry=%.2f SL=%.2f target=%.2f (RR 1:%.1f)%n", emaTrade.getStatus(),
				tick.getInstrumentKey(), tick.getTimestamp(), price, stopLoss, target, riskReward);
	}

	// =========================================================
	// MANAGE POSITION (trailing + exit rules)
	// =========================================================

	private void managePosition(Position p, TrendResult tick, double price, double bullish, double bearish) {

		// ---- best price update ----
		if (p.isLong) {
			p.extremePrice = Math.max(p.extremePrice, price);
		} else {
			p.extremePrice = Math.min(p.extremePrice, price);
		}

		double favorableMove = p.isLong ? p.extremePrice - p.entryPrice : p.entryPrice - p.extremePrice;

		// ---- trailing activate ----
		if (!p.trailing && favorableMove >= p.riskPerUnit * trailActivationR) {

			p.trailing = true;

			System.out.printf("TRAIL ON | %s | %s | price=%.2f%n", tick.getInstrumentKey(), tick.getTimestamp(),
					price);
		}

		// ---- trailing SL sirf favorable direction me move karta hai ----
		if (p.trailing) {

			double trailDistance = p.riskPerUnit * trailDistanceR;

			double newStop = p.isLong ? p.extremePrice - trailDistance : p.extremePrice + trailDistance;

			if (p.isLong && newStop > p.stopLoss) {
				p.stopLoss = newStop;
			}

			if (!p.isLong && newStop < p.stopLoss) {
				p.stopLoss = newStop;
			}
		}

		// ---- EXIT CHECKS ----

		boolean stopHit = p.isLong ? price <= p.stopLoss : price >= p.stopLoss;

		boolean targetHit = useFixedTarget && (p.isLong ? price >= p.target : price <= p.target);

		boolean signalWeak = p.isLong ? bullish < exitStrength : bearish < exitStrength;

		if (stopHit) {

			closePosition(p, tick, price, p.trailing ? "TRAILING_SL" : "STOP_LOSS");

		} else if (targetHit) {

			closePosition(p, tick, price, "TARGET");

		} else if (signalWeak) {

			closePosition(p, tick, price, "SIGNAL_EXIT");
		}
	}

	// =========================================================
	// CLOSE POSITION
	// =========================================================

	private void closePosition(Position p, TrendResult tick, double exitPrice, String reason) {

		EmaTrade trade = p.trade;

		if (p.isLong) {

			trade.setSellPrice(exitPrice);
			trade.setSellTime(tick.getTimestamp());

		} else {

			trade.setBuyPrice(exitPrice);
			trade.setBuyTime(tick.getTimestamp());
		}

		emaTradeRepository.save(trade);

		openPositions.remove(trade.getInstrumentKey());

		// Jab tak strength reset na ho, wahi side dobara enter nahi
		reentryBlock.add(trade.getInstrumentKey() + "|" + trade.getStatus());

		double pnl = p.isLong ? exitPrice - p.entryPrice : p.entryPrice - exitPrice;

		double rMultiple = p.riskPerUnit > 0 ? pnl / p.riskPerUnit : 0;

		System.out.printf("CLOSE %s | %s | %s | reason=%s | exit=%.2f | P&L/share=%.2f | %.2fR%n", trade.getStatus(),
				trade.getInstrumentKey(), tick.getTimestamp(), reason, exitPrice, pnl, rMultiple);
	}

	// =========================================================

	public List<EmaTrade> getAllTrades() {
		return emaTradeRepository.findAll();
	}

}