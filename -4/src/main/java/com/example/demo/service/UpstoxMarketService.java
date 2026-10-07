package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.model.DepthLevel;
import com.example.demo.model.MarketTick;
import com.example.demo.webSocket.MarketWebSocketHandler;

import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.auth.OAuth;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.MarketUpdateV3;
import com.upstox.feeder.constants.Mode;
import com.upstox.feeder.listener.OnMarketUpdateV3Listener;
import com.upstox.feeder.listener.OnOpenListener;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class UpstoxMarketService {

	@Value("${upstox.access-token}")
	private String accessToken;

	private final MarketWebSocketHandler browserHandler;

	private MarketDataStreamerV3 streamer;

	/*
	 * instrumentKey -> tradingSymbol
	 */
	private final Map<String, String> symbols = new HashMap<>();

	/*
	 * Upstox instrument master
	 */
	private static final String INSTRUMENT_MASTER_URL = "https://assets.upstox.com/market-quote/instruments/exchange/complete.json.gz";

	/*
	 * Nifty 50 index
	 */
	private static final String NIFTY_50_KEY = "NSE_INDEX|Nifty 50";

	/*
	 * ========================================================= NIFTY 50 STOCK
	 * SYMBOLS =========================================================
	 *
	 * Ye list sirf Nifty 50 stocks select karne ke liye hai.
	 *
	 * Upstox instrument master se inke actual instrument keys automatically find
	 * kiye jayenge.
	 *
	 */

	private static final Set<String> NIFTY_50_STOCKS = Set.of(

			"ADANIENT", "ADANIPORTS", "APOLLOHOSP", "ASIANPAINT", "AXISBANK", "BAJAJ-AUTO", "BAJFINANCE", "BAJAJFINSV",
			"BEL", "BHARTIARTL", "CIPLA", "COALINDIA", "DRREDDY", "EICHERMOT", "ETERNAL", "GRASIM", "HCLTECH",
			"HDFCBANK", "HDFCLIFE", "HEROMOTOCO", "HINDALCO", "HINDUNILVR", "ICICIBANK", "INDUSINDBK", "INFY", "ITC",
			"JIOFIN", "JSWSTEEL", "KOTAKBANK", "LT", "M&M", "MARUTI", "MAXHEALTH", "NESTLEIND", "NTPC", "ONGC",
			"POWERGRID", "RELIANCE", "SBILIFE", "SBIN", "SHRIRAMFIN", "SUNPHARMA", "TATACONSUM", "TATAMOTORS",
			"TATASTEEL", "TCS", "TECHM", "TITAN", "TRENT", "ULTRACEMCO", "WIPRO"

	);

	public UpstoxMarketService(MarketWebSocketHandler browserHandler) {

		this.browserHandler = browserHandler;
	}

	// =========================================================
	// START
	// =========================================================

	@PostConstruct
	public void startMarketFeed() {

		try {

			System.out.println();
			System.out.println("==============================================");
			System.out.println("STARTING UPSTOX MARKET SERVICE");
			System.out.println("==============================================");

			// =================================================
			// ACCESS TOKEN
			// =================================================

			if (accessToken == null || accessToken.isBlank() || accessToken.startsWith("${")) {

				System.err.println("UPSTOX ACCESS TOKEN NOT CONFIGURED");

				System.err.println("Set UPSTOX_ACCESS_TOKEN");

				return;
			}

			// =================================================
			// LOAD NIFTY 50 INSTRUMENTS
			// =================================================

			loadNifty50Instruments();

			if (symbols.isEmpty()) {

				System.err.println("NO NIFTY 50 INSTRUMENTS FOUND");

				return;
			}

			System.out.println();
			System.out.println("TOTAL NIFTY 50 INSTRUMENTS = " + symbols.size());

			// =================================================
			// CONNECT
			// =================================================

			connect();

		} catch (Exception e) {

			System.err.println("FAILED TO START UPSTOX MARKET SERVICE");

			e.printStackTrace();
		}
	}

	// =========================================================
	// LOAD ONLY NIFTY 50 STOCKS
	// =========================================================

	private void loadNifty50Instruments() throws Exception {

		System.out.println();
		System.out.println("DOWNLOADING UPSTOX INSTRUMENT MASTER...");

		URL url = URI.create(INSTRUMENT_MASTER_URL).toURL();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");

		connection.setConnectTimeout(30000);

		connection.setReadTimeout(60000);

		connection.setRequestProperty("Accept", "application/json");

		int responseCode = connection.getResponseCode();

		System.out.println("INSTRUMENT MASTER HTTP CODE = " + responseCode);

		if (responseCode != 200) {

			throw new RuntimeException("Unable to download instrument master. HTTP " + responseCode);
		}

		// =====================================================
		// GZIP
		// =====================================================

		try (

				java.util.zip.GZIPInputStream gzipInputStream = new java.util.zip.GZIPInputStream(
						connection.getInputStream());

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))

		) {

			StringBuilder jsonBuilder = new StringBuilder();

			String line;

			while ((line = reader.readLine()) != null) {

				jsonBuilder.append(line);
			}

			String json = jsonBuilder.toString();

			System.out.println("INSTRUMENT MASTER DOWNLOADED");

			JSONArray instruments = new JSONArray(json);

			System.out.println("TOTAL MASTER RECORDS = " + instruments.length());

			symbols.clear();

			// =================================================
			// FIND ONLY NIFTY 50 STOCKS
			// =================================================

			for (int i = 0; i < instruments.length(); i++) {

				JSONObject instrument = instruments.getJSONObject(i);

				String segment = instrument.optString("segment", "");

				String instrumentType = instrument.optString("instrument_type", "");

				String instrumentKey = instrument.optString("instrument_key", "");

				String tradingSymbol = instrument.optString("trading_symbol", "");

				// ---------------------------------------------
				// NSE EQUITY ONLY
				// ---------------------------------------------

				if (!"NSE_EQ".equals(segment)) {
					continue;
				}

				if (!"EQ".equals(instrumentType)) {
					continue;
				}

				if (instrumentKey.isBlank()) {
					continue;
				}

				if (tradingSymbol.isBlank()) {
					continue;
				}

				// ---------------------------------------------
				// NIFTY 50 ONLY
				// ---------------------------------------------

				if (!NIFTY_50_STOCKS.contains(tradingSymbol)) {

					continue;
				}

				symbols.put(instrumentKey, tradingSymbol);
			}
		}

		connection.disconnect();

		// =====================================================
		// ADD NIFTY 50 INDEX
		// =====================================================

		symbols.put(NIFTY_50_KEY, "NIFTY 50");

		// =====================================================
		// PRINT RESULT
		// =====================================================

//		System.out.println();
//		System.out.println("==============================================");
//
//		System.out.println("NIFTY 50 STOCKS FOUND = " + (symbols.size() - 1));
//
//		System.out.println("NIFTY 50 INDEX ADDED = " + symbols.containsKey(NIFTY_50_KEY));
//
//		System.out.println("TOTAL SUBSCRIPTION INSTRUMENTS = " + symbols.size());
//
//		System.out.println("==============================================");

		// =====================================================
		// PRINT SELECTED STOCKS
		// =====================================================

//		for (Map.Entry<String, String> entry : symbols.entrySet()) {
//
//			System.out.println(entry.getKey() + " -> " + entry.getValue());
//		}
	}

	// =========================================================
	// CONNECT UPSTOX
	// =========================================================

	private void connect() throws ApiException {

		ApiClient client = com.upstox.Configuration.getDefaultApiClient();

		OAuth oauth = (OAuth) client.getAuthentication("OAUTH2");

		oauth.setAccessToken(accessToken);

		// =====================================================
		// SELECT ONLY NIFTY 50
		// =====================================================

		Set<String> instrumentKeys = new HashSet<>(symbols.keySet());

		System.out.println();
		System.out.println("==============================================");

		System.out.println("SUBSCRIBING TO NIFTY 50");

		System.out.println("TOTAL INSTRUMENTS = " + instrumentKeys.size());

		System.out.println("==============================================");

		// =====================================================
		// CREATE STREAMER
		// =====================================================

		streamer = new MarketDataStreamerV3(client, instrumentKeys, Mode.FULL);

		// =====================================================
		// ON OPEN
		// =====================================================

		streamer.setOnOpenListener(new OnOpenListener() {

			@Override
			public void onOpen() {

				System.out.println("UPSTOX WEBSOCKET CONNECTED");

				System.out.println("NIFTY 50 MARKET FEED CONNECTED");
			}
		});

		// =====================================================
		// MARKET UPDATE
		// =====================================================

		streamer.setOnMarketUpdateListener(new OnMarketUpdateV3Listener() {

			@Override
			public void onUpdate(MarketUpdateV3 update) {

				processUpdate(update);
			}
		});

		// =====================================================
		// CONNECT
		// =====================================================

		streamer.connect();
	}

	// =========================================================
	// PROCESS UPDATE
	// =========================================================

	private void processUpdate(MarketUpdateV3 update) {

		try {

			if (update == null || update.getFeeds() == null) {

				return;
			}

			update.getFeeds().forEach((instrumentKey, feed) -> {

				try {

					// =================================================
					// SYMBOL
					// =================================================

					String symbol = symbols.getOrDefault(instrumentKey, instrumentKey);

					// =================================================
					// BASIC DATA
					// =================================================

					double ltp = 0;

					double previousClose = 0;

					double open = 0;

					double high = 0;

					double low = 0;

					long volume = 0;

					long lastQuantity = 0;

					// =================================================
					// TOTAL BUY / SELL
					// =================================================

					long totByQ = 0;

					long totSlQ = 0;

					// =================================================
					// DEPTH
					// =================================================

					List<DepthLevel> bidLevels = new ArrayList<>();

					List<DepthLevel> askLevels = new ArrayList<>();

					// =================================================
					// FULL FEED
					// =================================================

					if (feed.getFullFeed() != null) {

						// =================================================
						// INDEX FEED
						// =================================================

						if (feed.getFullFeed().getIndexFF() != null) {

							var indexFF = feed.getFullFeed().getIndexFF();

							// LTPC
							if (indexFF.getLtpc() != null) {

								var ltpc = indexFF.getLtpc();

								ltp = ltpc.getLtp();

								previousClose = ltpc.getCp();

								lastQuantity = ltpc.getLtq();
							}

							// OHLC
							if (indexFF.getMarketOHLC() != null) {

								var ohlcList = indexFF.getMarketOHLC().getOhlc();

								if (ohlcList != null && !ohlcList.isEmpty()) {

									var daily = ohlcList.stream().filter(o -> "1d".equals(o.getInterval())).findFirst()
											.orElse(ohlcList.get(0));

									open = daily.getOpen();

									high = daily.getHigh();

									low = daily.getLow();

									volume = daily.getVol();
								}
							}
						}

						// =================================================
						// EQUITY FEED
						// =================================================

						if (feed.getFullFeed().getMarketFF() != null) {

							var marketFF = feed.getFullFeed().getMarketFF();

							// LTPC
							if (marketFF.getLtpc() != null) {

								var ltpc = marketFF.getLtpc();

								ltp = ltpc.getLtp();

								previousClose = ltpc.getCp();

								lastQuantity = ltpc.getLtq();
							}

							// OHLC
							if (marketFF.getMarketOHLC() != null) {

								var ohlcList = marketFF.getMarketOHLC().getOhlc();

								if (ohlcList != null && !ohlcList.isEmpty()) {

									var daily = ohlcList.stream().filter(o -> "1d".equals(o.getInterval())).findFirst()
											.orElse(ohlcList.get(0));

									open = daily.getOpen();

									high = daily.getHigh();

									low = daily.getLow();

									volume = daily.getVol();
								}
							}

							// TOTAL BUY / SELL
							totByQ = (long) marketFF.getTbq();

							totSlQ = (long) marketFF.getTsq();

							// =================================================
							// 5 LEVEL DEPTH
							// =================================================

							if (marketFF.getMarketLevel() != null
									&& marketFF.getMarketLevel().getBidAskQuote() != null) {

								var quotes = marketFF.getMarketLevel().getBidAskQuote();

								for (var quote : quotes) {

									// BID
									if (quote.getBidP() != 0 || quote.getBidQ() != 0) {

										bidLevels.add(new DepthLevel(quote.getBidP(), quote.getBidQ()));
									}

									// ASK
									if (quote.getAskP() != 0 || quote.getAskQ() != 0) {

										askLevels.add(new DepthLevel(quote.getAskP(), quote.getAskQ()));
									}
								}
							}
						}
					}

					// =================================================
					// CHANGE
					// =================================================

					double change = ltp - previousClose;

					double changePercent = 0;

					if (previousClose != 0) {

						changePercent = (change / previousClose) * 100;
					}

					// =================================================
					// MARKET TICK
					// =================================================

					MarketTick tick = new MarketTick(

							instrumentKey,

							symbol,

							ltp,

							change,

							changePercent,

							open,

							high,

							low,

							previousClose,

							volume,

							lastQuantity,

							totByQ,

							totSlQ,

							String.valueOf(System.currentTimeMillis()),

							bidLevels,

							askLevels);

					// =================================================
					// SEND TO BROWSER
					// =================================================

					browserHandler.sendMarketData(tick);

				} catch (Exception e) {

					System.err.println("ERROR PROCESSING = " + instrumentKey);

					e.printStackTrace();
				}
			});

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public Map<String, String> getNifty50Stocks() {

		Map<String, String> result = new LinkedHashMap<>();

		for (Map.Entry<String, String> entry : symbols.entrySet()) {

			String instrumentKey = entry.getKey();

			String symbol = entry.getValue();

			// Only NSE Equity
			if (!instrumentKey.startsWith("NSE_EQ|")) {
				continue;
			}

			// Only Nifty 50 stocks
			if (NIFTY_50_STOCKS.contains(symbol)) {

				result.put(instrumentKey, symbol);
			}
		}

		return result;
	}

	// =========================================================
	// STOP
	// =========================================================

	@PreDestroy
	public void stopMarketFeed() {

		System.out.println();
		System.out.println("================================");

		System.out.println("STOPPING UPSTOX MARKET SERVICE");

		System.out.println("================================");

		try {

			if (streamer != null) {

				streamer.disconnect();

				System.out.println("UPSTOX WEBSOCKET DISCONNECTED");
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}