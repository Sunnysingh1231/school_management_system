package com.example.demo.service;

import com.example.demo.model.Candle;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SCALP STRATEGY RUNNER  (VWAP + EMA 9/21/50 + ATR + RSI + Volume, pullback-breakout)
 *
 * Trades DB me save NAHI hote. Sab kuch memory me (tradeLog list) rehta hai aur
 * ScalpController ke REST API se frontend ko milta hai.
 *
 * Isko chalane ke liye application.properties me:  scalp.enabled=true
 * (DayTradeEmaService ko band karo: daytrade.enabled=false, warna dono ek hi websocket pe push karenge)
 *
 * STRATEGY (LONG; SHORT bilkul ulta)
 *  Trend filter : close > VWAP, EMA9 > EMA21 > EMA50, EMA50 upar ki taraf slope
 *  Setup        : last 4 candles me price EMA21 zone tak pullback kar chuka ho
 *  Trigger      : current candle bullish + close > EMA9 + close > previous high (breakout)
 *  Confirmation : volume >= 1.2x avg(20), body >= 50% of range, RSI 52-70
 *  Filters      : ATR% min (dead market skip), over-extension skip, time windows,
 *                 daily loss limit, per-stock max trades, consecutive-loss stop, cooldown
 *  Stop loss    : pullback swing low - 0.1 ATR  (0.8 ATR se 2.0 ATR ke beech clamp)
 *  Exit         : 1R par 50% book + SL breakeven, baaki ATR trailing / 2.5R target / EMA9 exit,
 *                 time-stop, VWAP-fail exit, 15:15 square-off
 */
@Service
@ConditionalOnProperty(name = "scalp.enabled", havingValue = "true", matchIfMissing = false)
public class EmaVwapStrategyService {

	// =========================================================
	// CONFIG
	// =========================================================

	@Value("${scalp.date:}")
	private String tradeDateText;

	@Value("${scalp.interval-minutes:3}")
	private int intervalMinutes;

	@Value("${scalp.warmup-candles:300}")
	private int warmupCandles;

	@Value("${scalp.instruments:NSE_EQ|INE090A01021,NSE_EQ|INE062A01020,NSE_EQ|INE002A01018,NSE_EQ|INE467B01029,NSE_EQ|INE009A01021,NSE_EQ|INE397D01024,NSE_EQ|INE018A01030,NSE_EQ|INE238A01034,NSE_EQ|INE040A01034}")
	private String instrumentsCsv;

	@Value("${scalp.replay-delay-ms:300}")
	private long replayDelayMs;

	@Value("${scalp.poll-seconds:10}")
	private int pollSeconds;

	// ---- entry filters ----

	@Value("${scalp.volume-multiplier:1.2}")
	private double volumeMultiplier;

	@Value("${scalp.min-body-ratio:0.5}")
	private double minBodyRatio;

	@Value("${scalp.min-atr-percent:0.12}")
	private double minAtrPercent;

	@Value("${scalp.max-risk-percent:1.0}")
	private double maxRiskPercent;

	@Value("${scalp.rsi-long-min:52}")
	private double rsiLongMin;

	@Value("${scalp.rsi-long-max:70}")
	private double rsiLongMax;

	@Value("${scalp.rsi-short-min:30}")
	private double rsiShortMin;

	@Value("${scalp.rsi-short-max:48}")
	private double rsiShortMax;

	@Value("${scalp.pullback-zone-atr:0.3}")
	private double pullbackZoneAtr;

	@Value("${scalp.max-extension-atr:2.0}")
	private double maxExtensionAtr;

	// ---- time windows (candle start time) ----

	@Value("${scalp.window1-start:09:30}")
	private String window1StartText;

	@Value("${scalp.window1-end:11:30}")
	private String window1EndText;

	@Value("${scalp.window2-start:13:45}")
	private String window2StartText;

	@Value("${scalp.window2-end:14:45}")
	private String window2EndText;

	@Value("${scalp.squareoff-time:15:15}")
	private String squareOffTimeText;

	// ---- risk / exits ----

	@Value("${scalp.min-risk-atr:0.8}")
	private double minRiskAtr;

	@Value("${scalp.max-risk-atr:2.0}")
	private double maxRiskAtr;

	@Value("${scalp.swing-buffer-atr:0.1}")
	private double swingBufferAtr;

	@Value("${scalp.tp1-r:1.0}")
	private double tp1R;

	@Value("${scalp.tp2-r:2.5}")
	private double tp2R;

	@Value("${scalp.partial-fraction:0.5}")
	private double partialFraction;

	@Value("${scalp.breakeven-buffer-r:0.05}")
	private double breakevenBufferR;

	@Value("${scalp.trail-atr-mult:1.3}")
	private double trailAtrMult;

	@Value("${scalp.max-hold-candles:10}")
	private int maxHoldCandles;

	// ---- money / day controls ----

	@Value("${scalp.capital:100000}")
	private double capital;

	@Value("${scalp.risk-per-trade-percent:1.0}")
	private double riskPerTradePercent;

	@Value("${scalp.leverage:5}")
	private double leverage;

	@Value("${scalp.max-trades-per-stock:3}")
	private int maxTradesPerStock;

	@Value("${scalp.max-consecutive-losses:2}")
	private int maxConsecutiveLosses;

	@Value("${scalp.cooldown-candles:3}")
	private int cooldownCandles;

	@Value("${scalp.max-daily-loss-r:3.0}")
	private double maxDailyLossR;

	// =========================================================
	// CONSTANTS
	// =========================================================

	private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
	private static final String BASE_URL = "https://api.upstox.com/v3/historical-candle/";
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final int MAX_LOOKBACK_DAYS = 400;
	private static final LocalTime LIVE_STOP_TIME = LocalTime.of(15, 32);

	// =========================================================
	// DEPENDENCIES + STATE
	// =========================================================

	@Value("${upstox.access-token}")
	private String accessToken;

	private final WebSocketHandler webSocketHandler;
	private final EmaWebSocketHandler emaWebSocketHandler;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	private final HttpClient httpClient = HttpClient.newHttpClient();

	private final Map<String, InstrumentState> states = new LinkedHashMap<>();
	private final Map<String, Stats> statsByInstrument = new HashMap<>();

	/** DB ki jagah: sare trades (open + closed) yahin temporary store hote hain */
	private final List<TradeRecord> tradeLog = new CopyOnWriteArrayList<>();
	private final AtomicLong tradeIdSeq = new AtomicLong(1);

	/** Chart ke liye: har instrument ke processed candles + indicator values (memory me) */
	private final Map<String, List<CandlePoint>> seriesLog = new ConcurrentHashMap<>();

	private volatile boolean running = true;
	private volatile String modeText = "NOT_STARTED";
	private volatile String statusText = "idle";
	private Thread worker;

	private LocalTime w1Start, w1End, w2Start, w2End, squareOffTime;

	private LocalDate dayRDate;
	private double dayR;

	public EmaVwapStrategyService(WebSocketHandler webSocketHandler, EmaWebSocketHandler emaWebSocketHandler) {
		this.webSocketHandler = webSocketHandler;
		this.emaWebSocketHandler = emaWebSocketHandler;
	}

	// =========================================================
	// PUBLIC API (controller ke liye)
	// =========================================================

	public List<TradeRecord> getTrades() {
		return new ArrayList<>(tradeLog);
	}

	public List<TradeRecord> getOpenTrades() {
		List<TradeRecord> list = new ArrayList<>();
		for (TradeRecord t : tradeLog) {
			if ("OPEN".equals(t.status)) {
				list.add(t);
			}
		}
		return list;
	}

	/** Configured instrument keys (scalp.instruments) */
	public List<String> getInstrumentKeys() {

		List<String> keys = new ArrayList<>();

		for (String k : instrumentsCsv.split(",")) {
			if (!k.trim().isEmpty()) {
				keys.add(k.trim());
			}
		}

		return keys;
	}

	/**
	 * Chart data: from = frontend ke paas jitne candles pehle se hain.
	 * Sirf naye candles (index from ke baad ke) wapas aate hain.
	 */
	public Map<String, Object> getSeries(String instrumentKey, int from) {

		List<CandlePoint> all = seriesLog.get(instrumentKey);

		int total = all == null ? 0 : all.size();

		List<CandlePoint> part = new ArrayList<>();

		if (all != null && from >= 0 && from < total) {
			part.addAll(all.subList(from, total));
		}

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("total", total);
		m.put("candles", part);
		return m;
	}

	public Map<String, Object> getSummary() {

		int closed = 0, wins = 0, open = 0;
		double totalR = 0, pnlShare = 0, pnlAmount = 0;

		for (TradeRecord t : tradeLog) {

			if ("OPEN".equals(t.status)) {
				open++;
				continue;
			}

			closed++;
			totalR += t.rMultiple;
			pnlShare += t.pnlPerShare;
			pnlAmount += t.pnlAmount;

			if (t.pnlPerShare > 0) {
				wins++;
			}
		}

		Map<String, Object> m = new LinkedHashMap<>();
		m.put("mode", modeText);
		m.put("intervalMinutes", intervalMinutes);
		m.put("status", statusText);
		m.put("closedTrades", closed);
		m.put("openTrades", open);
		m.put("wins", wins);
		m.put("losses", closed - wins);
		m.put("winRatePercent", closed == 0 ? 0 : round2(wins * 100.0 / closed));
		m.put("totalR", round2(totalR));
		m.put("pnlPerShareTotal", round2(pnlShare));
		m.put("pnlAmount", round2(pnlAmount));
		m.put("note", "brokerage, taxes, slippage include nahi hain");
		return m;
	}

	// =========================================================
	// INNER CLASSES
	// =========================================================

	/** Chart ke liye ek candle + us candle tak ke indicator values */
	public static class CandlePoint {

		public LocalDateTime timestamp;
		public double open;
		public double high;
		public double low;
		public double close;
		public double volume;
		public Double ema9;
		public Double ema21;
		public Double ema50;
		public Double vwap;
		public Double rsi;
	}

	/** Frontend ko JSON me jaane wala trade (public fields => Jackson serialize kar deta hai) */
	public static class TradeRecord {

		public long id;
		public String instrumentKey;
		public String side; // BUY / SELL
		public String status; // OPEN / CLOSED
		public LocalDateTime entryTime;
		public double entryPrice;
		public double stopLoss; // current SL (trail ke saath badalta hai)
		public double initialStopLoss;
		public double target1;
		public double target2;
		public int quantity;
		public boolean partialBooked;
		public LocalDateTime exitTime;
		public double exitPrice; // aakhri leg ka exit
		public String exitReason;
		public double pnlPerShare; // blended (partial + final)
		public double pnlAmount;
		public double rMultiple;
		public String setup;
	}

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

	/** Wilder ATR */
	private static class AtrState {

		private final int period = 14;
		private double prevClose = Double.NaN;
		private int count;
		private double sum;
		private double value;

		void update(double high, double low, double close) {

			double tr = Double.isNaN(prevClose) ? high - low
					: Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));

			prevClose = close;

			if (count < period) {
				sum += tr;
				count++;
				if (count == period) {
					value = sum / period;
				}
			} else {
				value = (value * (period - 1) + tr) / period;
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

	/** Wilder RSI */
	private static class RsiState {

		private final int period = 14;
		private double prev = Double.NaN;
		private int count;
		private double gainSum, lossSum, avgGain, avgLoss;

		void update(double price) {

			if (Double.isNaN(prev)) {
				prev = price;
				return;
			}

			double change = price - prev;
			prev = price;

			double gain = Math.max(change, 0);
			double loss = Math.max(-change, 0);

			if (count < period) {
				gainSum += gain;
				lossSum += loss;
				count++;
				if (count == period) {
					avgGain = gainSum / period;
					avgLoss = lossSum / period;
				}
			} else {
				avgGain = (avgGain * (period - 1) + gain) / period;
				avgLoss = (avgLoss * (period - 1) + loss) / period;
				count++;
			}
		}

		boolean isReady() {
			return count >= period;
		}

		double getValue() {
			if (avgLoss == 0) {
				return 100;
			}
			return 100 - 100 / (1 + avgGain / avgLoss);
		}
	}

	private static class Position {

		final TradeRecord trade;
		final boolean isLong;
		final double entryPrice;
		final double risk; // 1R (price me)
		final double tp1;
		final double tp2;

		double stopLoss;
		double extreme;
		double remaining = 1.0;
		double realizedPnl = 0; // per share, sirf partial wala hissa
		boolean partialDone;
		int candlesHeld;

		Position(TradeRecord trade, boolean isLong, double entryPrice, double risk, double stopLoss, double tp1,
				double tp2) {

			this.trade = trade;
			this.isLong = isLong;
			this.entryPrice = entryPrice;
			this.risk = risk;
			this.stopLoss = stopLoss;
			this.tp1 = tp1;
			this.tp2 = tp2;
			this.extreme = entryPrice;
		}
	}

	private static class InstrumentState {

		final EmaState ema9 = new EmaState(9);
		final EmaState ema21 = new EmaState(21);
		final EmaState ema50 = new EmaState(50);
		final AtrState atr = new AtrState();
		final RsiState rsi = new RsiState();

		final Deque<Candle> recent = new ArrayDeque<>();
		final Deque<Double> ema50Hist = new ArrayDeque<>();
		final Deque<Double> volHist = new ArrayDeque<>();
		double volSum;

		LocalDate currentDay;
		double cumPV;
		double cumVol;

		Position position;
		Candle lastCandle;
		LocalDateTime lastProcessed;

		int tradesToday;
		int consecLosses;
		int cooldown;

		double avgVolume() {
			return volHist.isEmpty() ? 0 : volSum / volHist.size();
		}
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
		worker = new Thread(this::run, "scalp-strategy-runner");
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
				throw new IllegalArgumentException("scalp.interval-minutes 1 se 300 ke beech hona chahiye");
			}

			w1Start = LocalTime.parse(window1StartText.trim());
			w1End = LocalTime.parse(window1EndText.trim());
			w2Start = LocalTime.parse(window2StartText.trim());
			w2End = LocalTime.parse(window2EndText.trim());
			squareOffTime = LocalTime.parse(squareOffTimeText.trim());

			LocalDate today = LocalDate.now(IST);

			LocalDate tradeDate = tradeDateText == null || tradeDateText.isBlank() ? today
					: LocalDate.parse(tradeDateText.trim());

			if (tradeDate.isAfter(today)) {
				System.err.println("Trade date future ki hai: " + tradeDate);
				statusText = "error: future date";
				return;
			}

			boolean liveMode = tradeDate.equals(today);
			modeText = liveMode ? "LIVE" : "REPLAY";
			statusText = "warming up";

			System.out.println("==================================================");
			System.out.println("SCALP STRATEGY RUNNER");
			System.out.println("Trade date : " + tradeDate + (liveMode ? "  (LIVE)" : "  (REPLAY)"));
			System.out.println("Interval   : " + intervalMinutes + " min");
			System.out.println("==================================================");

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
				statusText = "error: warm-up failed";
				return;
			}

			statusText = "running";

			if (liveMode) {
				runLive();
			} else {
				runReplay(tradeDate);
			}

			for (InstrumentState s : states.values()) {
				if (s.position != null && s.lastCandle != null) {
					closePosition(s, s.lastCandle, s.lastCandle.getClose(), "END_OF_DATA");
				}
			}

			statusText = "finished";
			printSummary();

		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();
			statusText = "stopped";

		} catch (Exception e) {

			statusText = "error: " + e.getMessage();
			e.printStackTrace();
		}
	}

	// =========================================================
	// WARM-UP
	// =========================================================

	private void warmUp(String instrumentKey, LocalDate tradeDate) throws Exception {

		TreeMap<LocalDateTime, Candle> map = new TreeMap<>();

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
			feed(state, c);
			state.lastProcessed = c.getTimestamp();
		}

		states.put(instrumentKey, state);
		statsByInstrument.put(instrumentKey, new Stats());

		System.out.println("Warm-up " + instrumentKey + " -> " + warmup.size() + " candles | EMA50 ready = "
				+ state.ema50.isReady());
	}

	// =========================================================
	// REPLAY / LIVE
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
			System.err.println("Is date par koi candle nahi mila: " + tradeDate);
			return;
		}

		for (Map.Entry<LocalDateTime, List<Candle>> entry : timeline.entrySet()) {

			if (!running) {
				break;
			}

			for (Candle c : entry.getValue()) {
				processCandle(c);
			}

			if (replayDelayMs > 0) {
				Thread.sleep(replayDelayMs);
			}
		}
	}

	private void runLive() throws Exception {

		System.out.println("LIVE mode: har " + pollSeconds + " sec me naye candles check honge");

		while (running) {

			LocalDateTime now = LocalDateTime.now(IST);

			TreeMap<LocalDateTime, List<Candle>> batch = new TreeMap<>();

			for (Map.Entry<String, InstrumentState> entry : states.entrySet()) {

				try {

					for (Candle c : fetchIntraday(entry.getKey())) {

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
	// INDICATOR FEED
	// =========================================================

	/** Sirf indicators update (EMA/ATR/RSI/volume). VWAP alag se day-reset ke saath hota hai. */
	private void feed(InstrumentState s, Candle c) {

		double close = c.getClose();

		s.ema9.update(close);
		s.ema21.update(close);
		s.ema50.update(close);
		s.atr.update(c.getHigh(), c.getLow(), close);
		s.rsi.update(close);

		double vol = c.getVolume() * 1.0;
		s.volHist.addLast(vol);
		s.volSum += vol;

		if (s.volHist.size() > 20) {
			s.volSum -= s.volHist.removeFirst();
		}

		s.recent.addLast(c);

		if (s.recent.size() > 6) {
			s.recent.removeFirst();
		}

		if (s.ema50.isReady()) {

			s.ema50Hist.addLast(s.ema50.getValue());

			if (s.ema50Hist.size() > 6) {
				s.ema50Hist.removeFirst();
			}
		}
	}

	/** Chart ke liye candle + indicator values memory me store karo */
	private void recordPoint(Candle c, InstrumentState s, double vwap) {

		CandlePoint p = new CandlePoint();

		p.timestamp = c.getTimestamp();
		p.open = c.getOpen();
		p.high = c.getHigh();
		p.low = c.getLow();
		p.close = c.getClose();
		p.volume = c.getVolume() * 1.0;

		p.ema9 = s.ema9.isReady() ? Double.valueOf(round2(s.ema9.getValue())) : null;
		p.ema21 = s.ema21.isReady() ? Double.valueOf(round2(s.ema21.getValue())) : null;
		p.ema50 = s.ema50.isReady() ? Double.valueOf(round2(s.ema50.getValue())) : null;
		p.vwap = Double.valueOf(round2(vwap));
		p.rsi = s.rsi.isReady() ? Double.valueOf(round2(s.rsi.getValue())) : null;

		seriesLog.computeIfAbsent(c.getInstrumentKey(), k -> new CopyOnWriteArrayList<>()).add(p);
	}

	private boolean indicatorsReady(InstrumentState s) {
		return s.ema50.isReady() && s.atr.isReady() && s.rsi.isReady() && s.ema50Hist.size() >= 6
				&& s.volHist.size() >= 10;
	}

	// =========================================================
	// PROCESS ONE CANDLE
	// =========================================================

	private void processCandle(Candle c) {

		InstrumentState s = states.get(c.getInstrumentKey());

		if (s == null) {
			return;
		}

		LocalDate day = c.getTimestamp().toLocalDate();

		// naya din => VWAP aur daily counters reset
		if (!day.equals(s.currentDay)) {
			s.currentDay = day;
			s.cumPV = 0;
			s.cumVol = 0;
			s.tradesToday = 0;
			s.consecLosses = 0;
			s.cooldown = 0;
		}

		if (!day.equals(dayRDate)) {
			dayRDate = day;
			dayR = 0;
		}

		Candle prev = s.recent.peekLast();
		double volAvg = s.avgVolume(); // current candle se pehle ke 20 candles ka average

		feed(s, c);

		s.lastCandle = c;
		s.lastProcessed = c.getTimestamp();

		double typical = (c.getHigh() + c.getLow() + c.getClose()) / 3.0;
		double vol = c.getVolume() * 1.0;

		s.cumPV += typical * vol;
		s.cumVol += vol;

		double vwap = s.cumVol > 0 ? s.cumPV / s.cumVol : c.getClose();

		recordPoint(c, s, vwap);

		pushCandle(c);

		if (!indicatorsReady(s)) {
			return;
		}

		pushTrend(buildTrend(c, s, vwap));

		LocalTime time = c.getTimestamp().toLocalTime();

		if (s.position != null) {
			manageOpenPosition(s, c, vwap, time);
			return;
		}

		if (s.cooldown > 0) {
			s.cooldown--;
		}

		tryEntry(s, c, prev, volAvg, vwap, time);
	}

	// =========================================================
	// ENTRY LOGIC
	// =========================================================

	private boolean inEntryWindow(LocalTime t) {

		boolean w1 = !t.isBefore(w1Start) && t.isBefore(w1End);
		boolean w2 = !t.isBefore(w2Start) && t.isBefore(w2End);

		return (w1 || w2) && t.isBefore(squareOffTime);
	}

	private void tryEntry(InstrumentState s, Candle c, Candle prev, double volAvg, double vwap, LocalTime time) {

		if (prev == null || !inEntryWindow(time)) {
			return;
		}

		if (s.cooldown > 0 || s.tradesToday >= maxTradesPerStock || s.consecLosses >= maxConsecutiveLosses) {
			return;
		}

		if (dayR <= -maxDailyLossR) {
			return;
		}

		double close = c.getClose();
		double open = c.getOpen();
		double high = c.getHigh();
		double low = c.getLow();

		double e9 = s.ema9.getValue();
		double e21 = s.ema21.getValue();
		double e50 = s.ema50.getValue();
		double atr = s.atr.getValue();
		double rsi = s.rsi.getValue();

		if (close <= 0 || atr <= 0) {
			return;
		}

		// dead market filter
		if (atr / close * 100.0 < minAtrPercent) {
			return;
		}

		// volume confirmation
		double vol = c.getVolume() * 1.0;

		if (volAvg <= 0 || vol < volAvg * volumeMultiplier) {
			return;
		}

		// strong candle body
		double range = high - low;

		if (range <= 0 || Math.abs(close - open) / range < minBodyRatio) {
			return;
		}

		// EMA50 slope (5 candle pehle ke mukable)
		double ema50Old = s.ema50Hist.peekFirst();

		// last 4 candles (current included) ka pullback extreme
		double minLow = Double.MAX_VALUE;
		double maxHigh = -Double.MAX_VALUE;
		int n = 0;

		for (java.util.Iterator<Candle> it = s.recent.descendingIterator(); it.hasNext() && n < 4; n++) {
			Candle x = it.next();
			minLow = Math.min(minLow, x.getLow());
			maxHigh = Math.max(maxHigh, x.getHigh());
		}

		double zone = pullbackZoneAtr * atr;

		boolean longSetup = close > vwap && e9 > e21 && e21 > e50 && e50 > ema50Old && minLow <= e21 + zone
				&& close > e9 && close > open && close > prev.getHigh() && rsi >= rsiLongMin && rsi <= rsiLongMax
				&& (close - vwap) <= maxExtensionAtr * atr && (close - e21) <= 1.5 * atr * maxExtensionAtr / 2.0;

		boolean shortSetup = close < vwap && e9 < e21 && e21 < e50 && e50 < ema50Old && maxHigh >= e21 - zone
				&& close < e9 && close < open && close < prev.getLow() && rsi >= rsiShortMin && rsi <= rsiShortMax
				&& (vwap - close) <= maxExtensionAtr * atr && (e21 - close) <= 1.5 * atr * maxExtensionAtr / 2.0;

		if (longSetup) {
			openPosition(s, c, true, minLow, atr);
		} else if (shortSetup) {
			openPosition(s, c, false, maxHigh, atr);
		}
	}

	// =========================================================
	// OPEN POSITION
	// =========================================================

	private void openPosition(InstrumentState s, Candle c, boolean isLong, double swingExtreme, double atr) {

		double price = c.getClose();

		double rawRisk = isLong ? price - (swingExtreme - swingBufferAtr * atr)
				: (swingExtreme + swingBufferAtr * atr) - price;

		double risk = Math.max(minRiskAtr * atr, Math.min(maxRiskAtr * atr, rawRisk));

		if (risk <= 0 || risk / price * 100.0 > maxRiskPercent) {
			return;
		}

		// position size: capital ka riskPerTradePercent% risk, leverage cap ke andar
		double riskAmount = capital * riskPerTradePercent / 100.0;
		int qtyByRisk = (int) Math.floor(riskAmount / risk);
		int qtyByCapital = (int) Math.floor(capital * leverage / price);
		int qty = Math.min(qtyByRisk, qtyByCapital);

		if (qty < 1) {
			return;
		}

		double stopLoss = isLong ? price - risk : price + risk;
		double tp1 = isLong ? price + risk * tp1R : price - risk * tp1R;
		double tp2 = isLong ? price + risk * tp2R : price - risk * tp2R;

		TradeRecord t = new TradeRecord();

		t.id = tradeIdSeq.getAndIncrement();
		t.instrumentKey = c.getInstrumentKey();
		t.side = isLong ? "BUY" : "SELL";
		t.status = "OPEN";
		t.entryTime = c.getTimestamp();
		t.entryPrice = round2(price);
		t.stopLoss = round2(stopLoss);
		t.initialStopLoss = round2(stopLoss);
		t.target1 = round2(tp1);
		t.target2 = round2(tp2);
		t.quantity = qty;
		t.setup = isLong ? "PULLBACK_BREAKOUT_LONG" : "PULLBACK_BREAKDOWN_SHORT";

		tradeLog.add(t);

		s.position = new Position(t, isLong, price, risk, stopLoss, tp1, tp2);
		s.tradesToday++;

		System.out.printf("OPEN %s | %s | %s | entry=%.2f SL=%.2f TP1=%.2f TP2=%.2f qty=%d%n", t.side,
				t.instrumentKey, c.getTimestamp(), price, stopLoss, tp1, tp2, qty);
	}

	// =========================================================
	// MANAGE OPEN POSITION
	// =========================================================

	private void manageOpenPosition(InstrumentState s, Candle c, double vwap, LocalTime time) {

		Position p = s.position;
		TradeRecord t = p.trade;

		p.candlesHeld++;

		double high = c.getHigh();
		double low = c.getLow();
		double close = c.getClose();
		double e9 = s.ema9.getValue();
		double atr = s.atr.getValue();
		int dir = p.isLong ? 1 : -1;

		// 1) STOP (conservative: SL pehle check hota hai)
		boolean stopHit = p.isLong ? low <= p.stopLoss : high >= p.stopLoss;

		if (stopHit) {
			closePosition(s, c, p.stopLoss, p.partialDone ? "TRAILING_SL" : "STOP_LOSS");
			return;
		}

		boolean justPartial = false;

		if (!p.partialDone) {

			// 2) TP1: partial profit book + SL breakeven
			boolean tp1Hit = p.isLong ? high >= p.tp1 : low <= p.tp1;

			if (tp1Hit) {

				p.realizedPnl += partialFraction * dir * (p.tp1 - p.entryPrice);
				p.remaining = 1.0 - partialFraction;
				p.partialDone = true;
				justPartial = true;
				p.stopLoss = p.entryPrice + dir * breakevenBufferR * p.risk;

				t.partialBooked = true;
				t.stopLoss = round2(p.stopLoss);

				System.out.printf("PARTIAL | %s | %s | booked %.0f%% at %.2f, SL -> %.2f%n", t.instrumentKey,
						c.getTimestamp(), partialFraction * 100, p.tp1, p.stopLoss);
			}

		} else {

			// 3) TP2: baaki hissa target par
			boolean tp2Hit = p.isLong ? high >= p.tp2 : low <= p.tp2;

			if (tp2Hit) {
				closePosition(s, c, p.tp2, "TARGET");
				return;
			}
		}

		p.extreme = p.isLong ? Math.max(p.extreme, high) : Math.min(p.extreme, low);

		// 4) ATR TRAILING (partial ke baad)
		if (p.partialDone) {

			double newStop = p.isLong ? p.extreme - trailAtrMult * atr : p.extreme + trailAtrMult * atr;

			if (p.isLong && newStop > p.stopLoss) {
				p.stopLoss = newStop;
			}

			if (!p.isLong && newStop < p.stopLoss) {
				p.stopLoss = newStop;
			}

			t.stopLoss = round2(p.stopLoss);

			boolean closedThrough = p.isLong ? close <= p.stopLoss : close >= p.stopLoss;

			if (closedThrough) {
				closePosition(s, c, close, "TRAILING_SL");
				return;
			}

			// momentum khatam: close EMA9 ke opposite
			if (!justPartial && (p.isLong ? close < e9 : close > e9)) {
				closePosition(s, c, close, "EMA9_EXIT");
				return;
			}
		}

		// 5) INTRADAY SQUARE-OFF
		if (!time.isBefore(squareOffTime)) {
			closePosition(s, c, close, "EOD_SQUAREOFF");
			return;
		}

		if (!p.partialDone) {

			// 6) TIME STOP: scalp chala nahi => nikal jao
			double unrealR = dir * (close - p.entryPrice) / p.risk;

			if (p.candlesHeld >= maxHoldCandles && unrealR < 0.5) {
				closePosition(s, c, close, "TIME_STOP");
				return;
			}

			// 7) VWAP FAIL: trend invalid
			boolean vwapFail = p.isLong ? close < vwap - 0.1 * atr : close > vwap + 0.1 * atr;

			if (vwapFail) {
				closePosition(s, c, close, "VWAP_EXIT");
			}
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

		TradeRecord t = p.trade;
		int dir = p.isLong ? 1 : -1;

		double pnl = p.realizedPnl + p.remaining * dir * (exitPrice - p.entryPrice);
		double r = p.risk > 0 ? pnl / p.risk : 0;

		t.status = "CLOSED";
		t.exitTime = c.getTimestamp();
		t.exitPrice = round2(exitPrice);
		t.exitReason = reason;
		t.pnlPerShare = round2(pnl);
		t.pnlAmount = round2(pnl * t.quantity);
		t.rMultiple = round2(r);
		t.stopLoss = round2(p.stopLoss);

		s.position = null;
		s.cooldown = cooldownCandles;

		if (pnl > 0) {
			s.consecLosses = 0;
		} else {
			s.consecLosses++;
		}

		dayR += r;

		Stats stats = statsByInstrument.get(t.instrumentKey);

		if (stats != null) {
			stats.trades++;
			stats.pnl += pnl;
			stats.rTotal += r;
			if (pnl > 0) {
				stats.wins++;
			}
		}

		System.out.printf("CLOSE %s | %s | %s | reason=%s | exit=%.2f | P&L/share=%.2f | %.2fR%n", t.side,
				t.instrumentKey, c.getTimestamp(), reason, exitPrice, pnl, r);
	}

	// =========================================================
	// TREND (frontend ke existing format ke liye)
	// =========================================================

	/**
	 * Existing TrendResult format reuse kiya hai taaki frontend na toote.
	 * Mapping: ema20 slot = EMA9, ema50 slot = EMA21, ema200 slot = EMA50,
	 * above200 = price > VWAP.
	 */
	private TrendResult buildTrend(Candle c, InstrumentState s, double vwap) {

		double price = c.getClose();
		double e9 = s.ema9.getValue();
		double e21 = s.ema21.getValue();
		double e50 = s.ema50.getValue();

		boolean above9 = price > e9;
		boolean above21 = price > e21;
		boolean aboveVwap = price > vwap;

		boolean bullAlign = e9 > e21 && e21 > e50;
		boolean bearAlign = e9 < e21 && e21 < e50;

		double bullish = 0;
		double bearish = 0;

		if (above9) bullish += 20;
		if (above21) bullish += 20;
		if (aboveVwap) bullish += 20;
		if (bullAlign) bullish += 40;

		if (!above9) bearish += 20;
		if (!above21) bearish += 20;
		if (!aboveVwap) bearish += 20;
		if (bearAlign) bearish += 40;

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

		return new LiveEmaCalculatorService.TrendResult(c.getInstrumentKey(), price, e9, e21, e50, trend, bullish,
				bearish, above9, above21, aboveVwap, bullAlign, bearAlign, c.getTimestamp());
	}

	// =========================================================
	// SUMMARY
	// =========================================================

	private void printSummary() {

		System.out.println("==================== SCALP SUMMARY ====================");

		int trades = 0, wins = 0;
		double pnl = 0, r = 0;

		for (Map.Entry<String, Stats> entry : statsByInstrument.entrySet()) {

			Stats st = entry.getValue();

			if (st.trades == 0) {
				continue;
			}

			System.out.printf("%s | trades=%d wins=%d | P&L/share=%.2f | %.2fR%n", entry.getKey(), st.trades,
					st.wins, st.pnl, st.rTotal);

			trades += st.trades;
			wins += st.wins;
			pnl += st.pnl;
			r += st.rTotal;
		}

		System.out.printf("TOTAL | trades=%d wins=%d | P&L/share=%.2f | %.2fR%n", trades, wins, pnl, r);
		System.out.println("(brokerage, taxes, slippage include nahi hain)");
		System.out.println("=======================================================");
	}

	private static double round2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	// =========================================================
	// UPSTOX API
	// =========================================================

	private List<Candle> fetchHistorical(String instrumentKey, LocalDate from, LocalDate to) throws Exception {

		String encoded = URLEncoder.encode(instrumentKey, StandardCharsets.UTF_8);

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
	// WEBSOCKET PUSH (existing frontend ke same format me)
	// =========================================================

	private void pushCandle(Candle c) {
		try {
			webSocketHandler.sendMessage(objectMapper.writeValueAsString(c));
		} catch (Exception e) {
			// client na ho to ignore
		}
	}

	private void pushTrend(LiveEmaCalculatorService.TrendResult trend) {
		try {
			emaWebSocketHandler.sendMessage(objectMapper.writeValueAsString(trend));
		} catch (Exception e) {
			// client na ho to ignore
		}
	}
}