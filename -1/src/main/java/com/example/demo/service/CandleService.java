package com.example.demo.service;

import com.example.demo.model.Candle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CandleService {

    @Value("${upstox.access-token}")
    private String accessToken;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CandleService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    // =========================================================
    // ENCODE INSTRUMENT KEY
    // =========================================================
    private String encodeInstrumentKey(String instrumentKey) {

        if (instrumentKey == null) {
            return "";
        }

        return instrumentKey
                .replace("|", "%7C")
                .replace(" ", "%20");
    }

    // =========================================================
    // INTRADAY 15 MINUTE CANDLES
    // =========================================================
    public List<Candle> getIntraday15MinuteCandles(
            String instrumentKey) {

        String encodedKey =
                encodeInstrumentKey(instrumentKey);

        String url =
                "https://api.upstox.com/v3/historical-candle/intraday/"
                        + encodedKey
                        + "/minutes/15";

        return requestCandles(url);
    }

    // =========================================================
    // DAILY CANDLES
    // =========================================================
    public List<Candle> getDailyCandles(
            String instrumentKey) {

        LocalDate today = LocalDate.now();

        LocalDate fromDate =
                today.minusMonths(12);

        String encodedKey =
                encodeInstrumentKey(instrumentKey);

        String url =
                "https://api.upstox.com/v3/historical-candle/"
                        + encodedKey
                        + "/days/1/"
                        + today
                        + "/"
                        + fromDate;

        return requestCandles(url);
    }

    // =========================================================
    // REQUEST CANDLES
    // =========================================================
    private List<Candle> requestCandles(String url) {

        try {

//            System.out.println(
//                    "FETCHING CANDLES: " + url
//            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            // =================================================
            // API ERROR
            // =================================================
            if (response.statusCode() != 200) {

                System.err.println(
                        "CANDLE API ERROR "
                                + response.statusCode()
                                + " : "
                                + response.body()
                );

                return Collections.emptyList();
            }

            // =================================================
            // PARSE RESPONSE
            // =================================================
            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            JsonNode candles =
                    root.path("data")
                            .path("candles");

            List<Candle> result =
                    new ArrayList<>();

            if (!candles.isArray()) {

                System.err.println(
                        "CANDLE DATA IS NOT ARRAY: "
                                + response.body()
                );

                return result;
            }

            // =================================================
            // CONVERT CANDLES
            // =================================================
            for (JsonNode c : candles) {

                if (c.size() < 6) {
                    continue;
                }

                try {

                    ZonedDateTime timestamp =
                            ZonedDateTime.parse(
                                    c.get(0).asText()
                            );

                    double open =
                            c.get(1).asDouble();

                    double high =
                            c.get(2).asDouble();

                    double low =
                            c.get(3).asDouble();

                    double close =
                            c.get(4).asDouble();

                    long volume =
                            c.get(5).asLong();

                    result.add(
                            new Candle(
                                    timestamp,
                                    open,
                                    high,
                                    low,
                                    close,
                                    volume
                            )
                    );

                } catch (Exception candleException) {

                    System.err.println(
                            "INVALID CANDLE: " + c
                    );

                    candleException.printStackTrace();
                }
            }

            // Upstox candles generally come newest first.
            // Reverse so oldest -> newest.
            Collections.reverse(result);

//            System.out.println(
//                    "CANDLES FETCHED: "
//                            + result.size()
//                            + " | "
//                            + instrumentKeyFromUrl(url)
//            );

            return result;

        } catch (Exception e) {

            System.err.println(
                    "FAILED TO FETCH CANDLES: "
                            + url
            );

            e.printStackTrace();

            return Collections.emptyList();
        }
    }

    // =========================================================
    // ONLY FOR LOGGING
    // =========================================================
    private String instrumentKeyFromUrl(String url) {

        try {

            String[] parts =
                    url.split("/historical-candle/");

            if (parts.length > 1) {
                return parts[1];
            }

        } catch (Exception ignored) {
        }

        return url;
    }
}