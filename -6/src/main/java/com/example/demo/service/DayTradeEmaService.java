package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.model.EmaTrade;
import com.example.demo.repository.EmaTradeRepository;
import com.example.demo.service.LiveEmaCalculatorService.TrendResult;
import com.example.demo.webSocket.EmaWebSocketHandler;
import com.example.demo.webSocket.WebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DAY TRADE RUNNER
 *
 * 1) Trade date se pehle ke last N candles (default 1000) fetch karke EMA 20/50/200 warm-up karta hai.
 * 2) Phir us date ke candles ek ek karke process karta hai aur trade execute karta hai.
 *      - Date purani hai  -> REPLAY mode (candle by candle, replay-delay-ms ke gap se)
 *      - Date aaj ki hai  -> LIVE mode (naye completed candles poll karta hai)
 * 3) Date aur candle interval application.properties se manually change hote hain.
 */
@Service
@ConditionalOnProperty(name = "daytrade.enabled", havingValue = "true", matchIfMissing = true)
public class DayTradeEmaService {

	// =========================================================
	// CONFIG (application.properties se change karo)
	// =========================================================

	/** Trade date (yyyy-MM-dd). Khali chhodo to aaj ki date (LIVE mode) */
	@Value("${daytrade.date:}")
	private String tradeDateText;

	/** Candle interval minutes me (1 se 300). 1, 3, 5, 15... */
	@Value("${daytrade.interval-minutes:5}")
	private int intervalMinutes;

	/** Trade date se pehle kitne candles se EMA warm-up karna hai */
	@Value("${daytrade.warmup-candles:1000}")
	private int warmupCandles;

	/** Comma separated instrument keys */
	@Value("${daytrade.instruments:NSE_EQ|INE090A01021,NSE_EQ|INE062A01020,NSE_EQ|INE002A01018,NSE_EQ|INE467B01029,NSE_EQ|INE009A01021,NSE_EQ|INE397D01024,NSE_EQ|INE018A01030,NSE_EQ|INE238A01034,NSE_EQ|INE040A01034}")
	private String instrumentsCsv;

	/** Purani date replay me har candle ke beech delay (ms). 0 = instant */
	@Value("${daytrade.replay-delay-ms:500}")
	private long replayDelayMs;

	/** LIVE mode me kitne second baad naye candles check karne hain */
	@Value("${daytrade.poll-seconds:10}")
	private int pollSeconds;

	/** Closed trades DB me save karne hain ya nahi */
	@Value("${daytrade.save-to-db:true}")
	private boolean saveToDb;

	// ---- strategy ----

	@Value("${daytrade.entry-strength:90}")
	private double entryStrength;

	/**
	 * Signal exit threshold. 90 rakhoge to strength 100 se zara bhi giri to exit ho jayega
	 * (SL/trailing/target kam kaam karenge). Kam rakho (jaise 50) ya 0 se signal exit band karo.
	 */
	@Value("${daytrade.exit-strength:90}")
	private double exitStrength;

	/** 1R = entry price ka itna % */
	@Value("${daytrade.stoploss-percent:0.5}")
	private double stopLossPercent;

	/** Risk:Reward, 2.0 => target 2R */
	@Value("${daytrade.risk-reward:2.0}")
	private double riskReward;

	@Value("${daytrade.trail-activation-r:1.0}")
	private double trailActivationR;

	@Value("${daytrade.trail-distance-r:1.0}")
	private double trailDistanceR;

	@Value("${daytrade.use-fixed-target:true}")
	private boolean useFixedTarget;

	/** Is time (candle start) ke baad naya entry nahi */
	@Value("${daytrade.last-entry-time:15:00}")
	private String lastEntryTimeText;

	/** Is time (candle start) par open positions close (intraday square-off) */
	@Value("${daytrade.squareoff-time:15:15}")
	private String squareOffTimeText;

	// =========================================================
	// CONSTANTS
	// =========================================================

	private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

	private static final String BASE_URL = "https://api.upstox.com/v3/historical-candle/";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/** Warm-up ke liye maximum kitne calendar din peeche tak jana hai */
	private static final int MAX_LOOKBACK_DAYS = 400;

	/** Is time ke baad LIVE mode band (market 15:30 par close hota hai) */
	private static final LocalTime LIVE_STOP_TIME = LocalTime.of(15, 32);

	// =========================================================
	// DEPENDENCIES + STATE
	// =========================================================

	@Value("${upstox.access-token}")
	private String accessToken;

	private final EmaTradeRepository emaTradeRepository;
	private final WebSocketHandler webSocketHandler;
	private final EmaWebSocketHandler emaWebSocketHandler;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private final Map<String, InstrumentState> states = new LinkedHashMap<>();

	private final Map<String, Stats> statsByInstrument = new HashMap<>();

	private volatile boolean running = true;

	private Thread worker;

	private LocalTime lastEntryTime;
	private LocalTime squareOffTime;

	public DayTradeEmaService(EmaTradeRepository emaTradeRepository, WebSocketHandler webSocketHandler,
			EmaWebSocketHandler emaWebSocketHandler) {

		this.emaTradeRepository = emaTradeRepository;
		this.webSocketHandler = webSocketHandler;
		this.emaWebSocketHandler = emaWebSocketHandler;
	}

	// =========================================================
	// INNER CLASSES
	// =========================================================

	/** Incremental EMA: SMA seed, phir har candle par O(1) update */
	private static class EmaState {

		private final int period;
		private final double multiplier;
		private double sum;
		private int count;
		private double value;

		EmaState(int period) {
			this.period = period;
			this.multiplier = 2.0 / (period + 1);
		}

		void update(double price) {

			if (count < period) {

				sum += price;
				count++;

				if (count == period) {
					value = sum / period;
				}

			} else {

				value = (price - value) * multiplier + value;
				count++;
			}
		}

		boolean isReady() {
			return count >= period;
		}

		double getValue() {
			return value;
		}
	}

	private static class Position {

		final EmaTrade trade;
		final boolean isLong;
		final double entryPrice;
		final double riskPerUnit;
		final double target;

		double stopLoss;
		double extremePrice;
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

	private static class InstrumentState {

		final EmaState ema20 = new EmaState(20);
		final EmaState ema50 = new EmaState(50);
		final EmaState ema200 = new EmaState(200);

		Position position;
		Candle lastCandle;
		LocalDateTime lastProcessed;

		/** SL/target ke baad wahi side tab tak enter nahi jab tak strength reset na ho */
		boolean blockLong;
		boolean blockShort;
	}

	private static class Stats {

		int trades;
		int wins;
		double pnl;
		double rTotal;
	}

	// =========================================================
	// START / STOP
	// =========================================================

	@PostConstruct
	public void start() {

		worker = new Thread(this::run, "day-trade-runner");
		worker.setDaemon(true);
		worker.start();
	}

	@PreDestroy
	public void stop() {

		running = false;

		if (worker != null) {
			worker.interrupt();
		}
	}

	// =========================================================
	// MAIN FLOW
	// =========================================================

	private void run() {

		try {

			if (intervalMinutes < 1 || intervalMinutes > 300) {
				throw new IllegalArgumentException("daytrade.interval-minutes 1 se 300 ke beech hona chahiye");
			}

			lastEntryTime = LocalTime.parse(lastEntryTimeText.trim());
			squareOffTime = LocalTime.parse(squareOffTimeText.trim());

			LocalDate today = LocalDate.now(IST);

			LocalDate tradeDate = tradeDateText == null || tradeDateText.isBlank() ? today
					: LocalDate.parse(tradeDateText.trim());

			if (tradeDate.isAfter(today)) {
				System.err.println("Trade date future ki hai: " + tradeDate);
				return;
			}

			boolean liveMode = tradeDate.equals(today);

			System.out.println("==================================================");
			System.out.println("DAY TRADE RUNNER");
			System.out.println("Trade date : " + tradeDate + (liveMode ? "  (LIVE)" : "  (REPLAY)"));
			System.out.println("Interval   : " + intervalMinutes + " min");
			System.out.println("Warm-up    : " + warmupCandles + " candles");
			System.out.println("SL " + stopLossPercent + "% | RR 1:" + riskReward + " | trail " + trailActivationR
					+ "R/" + trailDistanceR + "R");
			System.out.println("==================================================");

			// ---------- 1) WARM-UP ----------

			for (String key : instrumentsCsv.split(",")) {

				String instrumentKey = key.trim();

				if (instrumentKey.isEmpty()) {
					continue;
				}

				try {

					warmUp(instrumentKey, tradeDate);

				} catch (Exception e) {

					System.err.println("Warm-up failed, skipping " + instrumentKey + " : " + e.getMessage());
				}
			}

			if (states.isEmpty()) {
				System.err.println("Kisi bhi instrument ka warm-up nahi hua. Token / date / network check karo.");
				return;
			}

			// ---------- 2) TRADE DATE ----------

			if (liveMode) {
				runLive();
			} else {
				runReplay(tradeDate);
			}

			// ---------- 3) END OF DATA: open positions band ----------

			for (InstrumentState s : states.values()) {

				if (s.position != null && s.lastCandle != null) {

					closePosition(s, s.lastCandle, s.lastCandle.getClose(), "END_OF_DATA");
				}
			}

			printSummary();

		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();

			System.out.println("Day trade runner stopped.");

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	// =========================================================
	// WARM-UP: trade date se pehle ke last N candles
	// =========================================================

	private void warmUp(String instrumentKey, LocalDate tradeDate) throws Exception {

		TreeMap<LocalDateTime, Candle> map = new TreeMap<>();

		/*
		 * Upstox limit (docs ke hisaab se): 1-15 min candles => max 1 month,
		 * 16+ min => max 1 quarter per request. Isliye chunks me fetch.
		 */
		int chunkDays = intervalMinutes <= 15 ? 28 : 85;

		LocalDate limit = tradeDate.minusDays(MAX_LOOKBACK_DAYS);

		LocalDate chunkTo = tradeDate.minusDays(1);

		while (map.size() < warmupCandles && !chunkTo.isBefore(limit)) {

			LocalDate chunkFrom = chunkTo.minusDays(chunkDays - 1);

			for (Candle c : fetchHistorical(instrumentKey, chunkFrom, chunkTo)) {
				map.put(c.getTimestamp(), c);
			}

			chunkTo = chunkFrom.minusDays(1);

			Thread.sleep(150);
		}

		List<Candle> all = new ArrayList<>(map.values());

		List<Candle> warmup = all.size() > warmupCandles ? all.subList(all.size() - warmupCandles, all.size()) : all;

		InstrumentState state = new InstrumentState();

		for (Candle c : warmup) {

			updateEmas(state, c.getClose());

			state.lastProcessed = c.getTimestamp();
		}

		states.put(instrumentKey, state);

		statsByInstrument.put(instrumentKey, new Stats());

		if (warmup.isEmpty()) {

			System.out.println("Warm-up " + instrumentKey + " -> 0 candles");

		} else {

			System.out.println("Warm-up " + instrumentKey + " -> " + warmup.size() + " candles ("
					+ warmup.get(0).getTimestamp() + " to " + warmup.get(warmup.size() - 1).getTimestamp()
					+ ") | EMA200 ready = " + state.ema200.isReady());
		}
	}

	// =========================================================
	// REPLAY MODE (purani date)
	// =========================================================

	private void runReplay(LocalDate tradeDate) throws Exception {

		TreeMap<LocalDateTime, List<Candle>> timeline = new TreeMap<>();

		for (String instrumentKey : states.keySet()) {

			try {

				for (Candle c : fetchHistorical(instrumentKey, tradeDate, tradeDate)) {

					timeline.computeIfAbsent(c.getTimestamp(), t -> new ArrayList<>()).add(c);
				}

				Thread.sleep(150);

			} catch (Exception e) {

				System.err.println("Trade date data failed " + instrumentKey + " : " + e.getMessage());
			}
		}

		if (timeline.isEmpty()) {

			System.err.println("Is date par koi candle nahi mila (weekend/holiday ya data available nahi): " + tradeDate);

			return;
		}

		int total = timeline.size();
		int index = 0;

		for (Map.Entry<LocalDateTime, List<Candle>> entry : timeline.entrySet()) {

			if (!running) {
				break;
			}

			for (Candle c : entry.getValue()) {
				processCandle(c);
			}

			index++;

			System.out.println("Candle " + index + "/" + total + "  " + entry.getKey());

			if (replayDelayMs > 0) {
				Thread.sleep(replayDelayMs);
			}
		}
	}

	// =========================================================
	// LIVE MODE (aaj ki date): naye completed candles poll karo
	// =========================================================

	private void runLive() throws Exception {

		System.out.println("LIVE mode: har " + pollSeconds + " sec me naye candles check honge");

		while (running) {

			LocalDateTime now = LocalDateTime.now(IST);

			TreeMap<LocalDateTime, List<Candle>> batch = new TreeMap<>();

			for (Map.Entry<String, InstrumentState> entry : states.entrySet()) {

				try {

					for (Candle c : fetchIntraday(entry.getKey())) {

						// Sirf poora ban chuka candle (forming candle par trade nahi)
						boolean completed = !c.getTimestamp().plusMinutes(intervalMinutes).isAfter(now);

						LocalDateTime last = entry.getValue().lastProcessed;

						boolean isNew = last == null || c.getTimestamp().isAfter(last);

						if (completed && isNew) {

							batch.computeIfAbsent(c.getTimestamp(), t -> new ArrayList<>()).add(c);
						}
					}

				} catch (Exception e) {

					System.err.println("Live fetch failed " + entry.getKey() + " : " + e.getMessage());
				}
			}

			for (List<Candle> group : batch.values()) {

				for (Candle c : group) {
					processCandle(c);
				}
			}

			if (now.toLocalTime().isAfter(LIVE_STOP_TIME)) {

				System.out.println("Market close ho gaya, LIVE mode band.");

				break;
			}

			Thread.sleep(pollSeconds * 1000L);
		}
	}

	// =========================================================
	// PROCESS ONE CANDLE (EMA update -> trend -> trade logic)
	// =========================================================

	private void processCandle(Candle c) {

		InstrumentState s = states.get(c.getInstrumentKey());

		if (s == null) {
			return;
		}

		updateEmas(s, c.getClose());

		s.lastCandle = c;
		s.lastProcessed = c.getTimestamp();

		pushCandle(c);

		if (!s.ema200.isReady()) {

			System.out.println("EMA200 ready nahi hai, warm-up candles kam hain : " + c.getInstrumentKey());

			return;
		}

		TrendResult trend = buildTrend(c, s);

		pushTrend(trend);

		LocalTime time = c.getTimestamp().toLocalTime();

		double bullish = trend.getBullishStrength();
		double bearish = trend.getBearishStrength();

		// strength reset => wahi side dobara allowed
		if (bullish < entryStrength) {
			s.blockLong = false;
		}

		if (bearish < entryStrength) {
			s.blockShort = false;
		}

		// ---- open position: pehle exit rules ----
		if (s.position != null) {

			manageOpenPosition(s, c, trend, time);

			return; // exit wale candle par naya entry nahi
		}

		// ---- entry (signal candle ke close par) ----
		if (time.isBefore(lastEntryTime) && time.isBefore(squareOffTime)) {

			if (bullish > entryStrength && !s.blockLong) {

				openPosition(s, c, trend, true);

			} else if (bearish > entryStrength && !s.blockShort) {

				openPosition(s, c, trend, false);
			}
		}
	}

	private void updateEmas(InstrumentState s, double close) {

		s.ema20.update(close);
		s.ema50.update(close);
		s.ema200.update(close);
	}

	// =========================================================
	// TREND (LiveEmaCalculatorService jaisa hi strength logic)
	// =========================================================

	private LiveEmaCalculatorService.TrendResult buildTrend(Candle c, InstrumentState s) {

		double price = c.getClose();

		double e20 = s.ema20.getValue();
		double e50 = s.ema50.getValue();
		double e200 = s.ema200.getValue();

		boolean above20 = price > e20;
		boolean above50 = price > e50;
		boolean above200 = price > e200;

		boolean bullishAlignment = e20 > e50 && e50 > e200;
		boolean bearishAlignment = e20 < e50 && e50 < e200;

		double bullish = 0;
		double bearish = 0;

		if (above20) bullish += 20;
		if (above50) bullish += 20;
		if (above200) bullish += 20;
		if (bullishAlignment) bullish += 40;

		if (!above20) bearish += 20;
		if (!above50) bearish += 20;
		if (!above200) bearish += 20;
		if (bearishAlignment) bearish += 40;

		LiveEmaCalculatorService.TrendType trend;

		if (bullish >= 80) {
			trend = LiveEmaCalculatorService.TrendType.STRONG_BULLISH;
		} else if (bullish >= 60) {
			trend = LiveEmaCalculatorService.TrendType.BULLISH;
		} else if (bearish >= 80) {
			trend = LiveEmaCalculatorService.TrendType.STRONG_BEARISH;
		} else if (bearish >= 60) {
			trend = LiveEmaCalculatorService.TrendType.BEARISH;
		} else {
			trend = LiveEmaCalculatorService.TrendType.SIDEWAYS;
		}

		return new LiveEmaCalculatorService.TrendResult(c.getInstrumentKey(), price, e20, e50, e200, trend, bullish,
				bearish, above20, above50, above200, bullishAlignment, bearishAlignment, c.getTimestamp());
	}

	// =========================================================
	// OPEN POSITION
	// =========================================================

	private void openPosition(InstrumentState s, Candle c, LiveEmaCalculatorService.TrendResult trend,
			boolean isLong) {

		double price = c.getClose();

		if (price <= 0) {
			return;
		}

		double risk = price * stopLossPercent / 100.0;

		double stopLoss = isLong ? price - risk : price + risk;

		double target = isLong ? price + risk * riskReward : price - risk * riskReward;

		EmaTrade trade = new EmaTrade();

		trade.setInstrumentKey(c.getInstrumentKey());
		trade.setTrend(String.valueOf(trend.getTrend()));

		if (isLong) {

			trade.setStatus("BUY");
			trade.setBuyPrice(price);
			trade.setBuyTime(c.getTimestamp());

		} else {

			trade.setStatus("SELL");
			trade.setSellPrice(price);
			trade.setSellTime(c.getTimestamp());
		}

		s.position = new Position(trade, isLong, price, risk, stopLoss, target);

		System.out.printf("OPEN %s | %s | %s | entry=%.2f SL=%.2f target=%.2f (RR 1:%.1f)%n", trade.getStatus(),
				c.getInstrumentKey(), c.getTimestamp(), price, stopLoss, target, riskReward);
	}

	// =========================================================
	// MANAGE OPEN POSITION (candle high/low se SL, target, trailing)
	// =========================================================

	private void manageOpenPosition(InstrumentState s, Candle c, LiveEmaCalculatorService.TrendResult trend,
			LocalTime time) {

		Position p = s.position;

		double high = c.getHigh();
		double low = c.getLow();
		double close = c.getClose();

		// 1) STOP: candle shuru hone par jo SL tha. SL pehle check hota hai (conservative:
		//    agar ek hi candle me SL aur target dono touch ho to SL maana jata hai)
		boolean stopHit = p.isLong ? low <= p.stopLoss : high >= p.stopLoss;

		if (stopHit) {

			closePosition(s, c, p.stopLoss, p.trailing ? "TRAILING_SL" : "STOP_LOSS");

			return;
		}

		// 2) TARGET
		if (useFixedTarget) {

			boolean targetHit = p.isLong ? high >= p.target : low <= p.target;

			if (targetHit) {

				closePosition(s, c, p.target, "TARGET");

				return;
			}
		}

		// 3) TRAILING update (agle candle ke liye naya SL)
		p.extremePrice = p.isLong ? Math.max(p.extremePrice, high) : Math.min(p.extremePrice, low);

		double favorableMove = p.isLong ? p.extremePrice - p.entryPrice : p.entryPrice - p.extremePrice;

		if (!p.trailing && favorableMove >= p.riskPerUnit * trailActivationR) {

			p.trailing = true;

			System.out.printf("TRAIL ON | %s | %s%n", c.getInstrumentKey(), c.getTimestamp());
		}

		if (p.trailing) {

			double trailDistance = p.riskPerUnit * trailDistanceR;

			double newStop = p.isLong ? p.extremePrice - trailDistance : p.extremePrice + trailDistance;

			if (p.isLong && newStop > p.stopLoss) {
				p.stopLoss = newStop;
			}

			if (!p.isLong && newStop < p.stopLoss) {
				p.stopLoss = newStop;
			}

			// Candle ka close hi naye SL ke paar chala gaya => close par exit (conservative)
			boolean closedThrough = p.isLong ? close <= p.stopLoss : close >= p.stopLoss;

			if (closedThrough) {

				closePosition(s, c, close, "TRAILING_SL");

				return;
			}
		}

		// 4) INTRADAY SQUARE-OFF
		if (!time.isBefore(squareOffTime)) {

			closePosition(s, c, close, "EOD_SQUAREOFF");

			return;
		}

		// 5) SIGNAL EXIT
		boolean signalWeak = p.isLong ? trend.getBullishStrength() < exitStrength
				: trend.getBearishStrength() < exitStrength;

		if (signalWeak) {

			closePosition(s, c, close, "SIGNAL_EXIT");
		}
	}

	// =========================================================
	// CLOSE POSITION
	// =========================================================

	private void closePosition(InstrumentState s, Candle c, double exitPrice, String reason) {

		Position p = s.position;

		if (p == null) {
			return;
		}

		EmaTrade trade = p.trade;

		if (p.isLong) {

			trade.setSellPrice(exitPrice);
			trade.setSellTime(c.getTimestamp());

		} else {

			trade.setBuyPrice(exitPrice);
			trade.setBuyTime(c.getTimestamp());
		}

		if (saveToDb) {

			try {

				emaTradeRepository.save(trade);

			} catch (Exception e) {

				System.err.println("Trade DB save failed : " + e.getMessage());
			}
		}

		s.position = null;

		if (p.isLong) {
			s.blockLong = true;
		} else {
			s.blockShort = true;
		}

		double pnl = p.isLong ? exitPrice - p.entryPrice : p.entryPrice - exitPrice;

		double rMultiple = p.riskPerUnit > 0 ? pnl / p.riskPerUnit : 0;

		Stats stats = statsByInstrument.get(trade.getInstrumentKey());

		if (stats != null) {

			stats.trades++;
			stats.pnl += pnl;
			stats.rTotal += rMultiple;

			if (pnl > 0) {
				stats.wins++;
			}
		}

		System.out.printf("CLOSE %s | %s | %s | reason=%s | exit=%.2f | P&L/share=%.2f | %.2fR%n",
				trade.getStatus(), trade.getInstrumentKey(), c.getTimestamp(), reason, exitPrice, pnl, rMultiple);
	}

	// =========================================================
	// SUMMARY
	// =========================================================

	private void printSummary() {

		System.out.println("==================== DAY SUMMARY ====================");

		int trades = 0;
		int wins = 0;
		double pnl = 0;
		double r = 0;

		for (Map.Entry<String, Stats> entry : statsByInstrument.entrySet()) {

			Stats st = entry.getValue();

			if (st.trades == 0) {
				continue;
			}

			System.out.printf("%s | trades=%d wins=%d | P&L/share=%.2f | %.2fR%n", entry.getKey(), st.trades, st.wins,
					st.pnl, st.rTotal);

			trades += st.trades;
			wins += st.wins;
			pnl += st.pnl;
			r += st.rTotal;
		}

		System.out.printf("TOTAL | trades=%d wins=%d | P&L/share=%.2f | %.2fR%n", trades, wins, pnl, r);

		System.out.println("(brokerage, taxes, slippage include nahi hain)");

		System.out.println("=====================================================");
	}

	// =========================================================
	// UPSTOX API
	// =========================================================

	private List<Candle> fetchHistorical(String instrumentKey, LocalDate from, LocalDate to) throws Exception {

		String encoded = URLEncoder.encode(instrumentKey, StandardCharsets.UTF_8);

		// Upstox format: /{instrumentKey}/minutes/{interval}/{to_date}/{from_date}
		String url = BASE_URL + encoded + "/minutes/" + intervalMinutes + "/" + to.format(DATE_FORMAT) + "/"
				+ from.format(DATE_FORMAT);

		return fetchCandles(url, instrumentKey);
	}

	private List<Candle> fetchIntraday(String instrumentKey) throws Exception {

		String encoded = URLEncoder.encode(instrumentKey, StandardCharsets.UTF_8);

		String url = BASE_URL + "intraday/" + encoded + "/minutes/" + intervalMinutes;

		return fetchCandles(url, instrumentKey);
	}

	private List<Candle> fetchCandles(String url, String instrumentKey) throws Exception {

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("Authorization", "Bearer " + accessToken).header("Accept", "application/json").GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {

			throw new IllegalStateException(
					"Upstox API " + response.statusCode() + " | " + url + " | " + response.body());
		}

		JsonNode candles = objectMapper.readTree(response.body()).path("data").path("candles");

		List<Candle> result = new ArrayList<>();

		if (!candles.isArray()) {
			return result;
		}

		for (JsonNode candle : candles) {

			if (candle.size() < 6) {
				continue;
			}

			// 2026-09-30T09:15:00+05:30 -> offset hata ke exchange local time
			LocalDateTime timestamp = OffsetDateTime.parse(candle.get(0).asText()).toLocalDateTime();

			double open = candle.get(1).asDouble();
			double high = candle.get(2).asDouble();
			double low = candle.get(3).asDouble();
			double close = candle.get(4).asDouble();
			long volume = candle.get(5).asLong();

			result.add(new Candle(instrumentKey, timestamp, open, high, low, close, volume));
		}

		return result;
	}

	// =========================================================
	// WEBSOCKET PUSH (tumhare existing frontend ke liye same format)
	// =========================================================

	private void pushCandle(Candle c) {

		try {

			webSocketHandler.sendMessage(objectMapper.writeValueAsString(c));

		} catch (Exception e) {

			// websocket client na ho to ignore
		}
	}

	private void pushTrend(LiveEmaCalculatorService.TrendResult trend) {

		try {

			emaWebSocketHandler.sendMessage(objectMapper.writeValueAsString(trend));

		} catch (Exception e) {

			// websocket client na ho to ignore
		}
	}
}