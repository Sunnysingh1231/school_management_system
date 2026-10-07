package com.example.demo.service;

import com.example.demo.model.Candle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class Nifty50FiveMinuteHistoricalService {

	@Value("${upstox.access-token}")
	private String accessToken;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	// =========================================================
	// GET PREVIOUS 20 TRADING DAYS - 5 MINUTE CANDLES
	// =========================================================

	public List<Candle> getPrevious20Days5Minute(String instrumentKey) throws Exception {

		List<Candle> allCandles = new ArrayList<>();

		/*
		 * 20 trading days approximately 28-30 calendar days hote hain.
		 *
		 * 40 calendar days le rahe hain taaki weekends + holidays cover ho jayein.
		 */

		LocalDate toDate = LocalDate.now().minusDays(1);

		LocalDate fromDate = LocalDate.now().minusDays(10);

		// =====================================================
		// ENCODE INSTRUMENT KEY
		// =====================================================

		String encodedInstrumentKey = URLEncoder.encode(instrumentKey, StandardCharsets.UTF_8);

		// =====================================================
		// UPSTOX HISTORICAL API URL
		// =====================================================

		String url = "https://api.upstox.com/v3/historical-candle/" + encodedInstrumentKey + "/minutes/5/"
				+ toDate.format(DATE_FORMAT) + "/" + fromDate.format(DATE_FORMAT);

//        System.out.println();
//        System.out.println(
//                "Fetching 5M data : "
//                        + instrumentKey
//        );
//
//        System.out.println(
//                "From : " + fromDate
//                        + " To : " + toDate
//        );

		// =====================================================
		// HTTP REQUEST
		// =====================================================

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("Authorization", "Bearer " + accessToken).header("Accept", "application/json").GET().build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		// =====================================================
		// CHECK RESPONSE
		// =====================================================

		if (response.statusCode() != 200) {

			System.err.println("Upstox API Error : " + response.statusCode());

			System.err.println(response.body());

			return allCandles;
		}

		// =====================================================
		// PARSE JSON
		// =====================================================

		JsonNode root = objectMapper.readTree(response.body());

		JsonNode candles = root.path("data").path("candles");

		if (!candles.isArray()) {

			System.err.println("No candle data found : " + instrumentKey);

			return allCandles;
		}

		// =====================================================
		// PARSE EVERY 5-MINUTE CANDLE
		// =====================================================

		for (JsonNode candle : candles) {

			if (candle.size() < 6) {
				continue;
			}

			// -------------------------------------------------
			// TIMESTAMP
			// -------------------------------------------------

			String timestampText = candle.get(0).asText();

			/*
			 * Upstox timestamp example:
			 *
			 * 2026-09-30T09:15:00+05:30
			 *
			 * Pehle OffsetDateTime parse karenge.
			 */

			OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestampText);

			/*
			 * Offset remove karke LocalDateTime banayenge.
			 *
			 * Result:
			 *
			 * 2026-09-30T09:15
			 */

			LocalDateTime timestamp = offsetDateTime.toLocalDateTime();

			// -------------------------------------------------
			// OHLC
			// -------------------------------------------------

			double open = candle.get(1).asDouble();

			double high = candle.get(2).asDouble();

			double low = candle.get(3).asDouble();

			double close = candle.get(4).asDouble();

			// -------------------------------------------------
			// VOLUME
			// -------------------------------------------------

			long volume = candle.get(5).asLong();

			// =================================================
			// CREATE CANDLE OBJECT
			// =================================================

			Candle candleObject = new Candle(instrumentKey, timestamp, open, high, low, close, volume);

			allCandles.add(candleObject);
		}

		// =====================================================
		// SORT OLDEST -> NEWEST
		// =====================================================

		allCandles.sort(Comparator.comparing(Candle::getTimestamp));

		// =====================================================
		// GET UNIQUE TRADING DATES
		// =====================================================

		List<LocalDate> tradingDates = allCandles.stream()

				.map(candle -> candle.getTimestamp().toLocalDate())

				.distinct()

				.sorted()

				.toList();

		// =====================================================
		// KEEP ONLY LATEST 20 TRADING DAYS
		// =====================================================

		if (tradingDates.size() > 20) {

			tradingDates = tradingDates.subList(tradingDates.size() - 20, tradingDates.size());
		}

		// =====================================================
		// FILTER CANDLES FOR THOSE 20 DAYS
		// =====================================================

		List<Candle> finalCandles = new ArrayList<>();

		for (Candle candle : allCandles) {

			LocalDate candleDate = candle.getTimestamp().toLocalDate();

			if (tradingDates.contains(candleDate)) {

				finalCandles.add(candle);
			}
		}

		// =====================================================
		// FINAL SORT
		// =====================================================

		finalCandles.sort(Comparator.comparing(Candle::getTimestamp));

//        System.out.println(
//                "Trading days : "
//                        + tradingDates.size()
//        );
//
//        System.out.println(
//                "5M candles   : "
//                        + finalCandles.size()
//        );

		return finalCandles;
	}
}