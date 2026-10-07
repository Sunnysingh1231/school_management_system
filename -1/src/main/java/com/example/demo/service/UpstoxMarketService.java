package com.example.demo.service;

import com.example.demo.model.DepthLevel;
import com.example.demo.model.MarketTick;
import com.example.demo.templates.MarketWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.zip.GZIPInputStream;

@Service
public class UpstoxMarketService {

    private final MarketDataStore marketDataStore;
    private final MarketWebSocketHandler browserHandler;

    @Value("${upstox.access-token}")
    private String accessToken;

    private MarketDataStreamerV3 streamer;

    // =========================================================
    // INSTRUMENT MAP
    // =========================================================

    private final Map<String, String> symbols =
            new LinkedHashMap<>();

    // =========================================================
    // FULL SUBSCRIPTION LIMIT
    // =========================================================

    private static final int MAX_FULL_INSTRUMENTS = 200;

    // =========================================================
    // LATEST MARKET DATA
    // =========================================================

    /*
     * Har instrument ka sirf latest tick memory me rahega.
     *
     * Upstox:
     *
     *  RELIANCE -> tick1
     *  RELIANCE -> tick2
     *  RELIANCE -> tick3
     *
     * Browser ko teenon immediately bhejne ke bajay
     * sirf latest tick bheja jayega.
     */
    private final ConcurrentMap<String, MarketTick> latestTicks =
            new ConcurrentHashMap<>();

    /*
     * Kaunse instruments me naya update aaya hai.
     */
    private final Set<String> dirtyInstruments =
            ConcurrentHashMap.newKeySet();

    // =========================================================
    // BROWSER SENDER THREAD
    // =========================================================

    private final ScheduledExecutorService browserSender =
            Executors.newSingleThreadScheduledExecutor(r -> {

                Thread thread = new Thread(
                        r,
                        "market-browser-sender"
                );

                thread.setDaemon(true);

                return thread;
            });

    // Browser ko kitne milliseconds me latest data bhejna hai.
    //
    // 100 ms = approx 10 cycles/sec
    //
    // Agar bahut zyada load ho to 200/250 ms kar sakte ho.
    private static final long BROWSER_SEND_INTERVAL_MS = 100;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public UpstoxMarketService(
            MarketWebSocketHandler browserHandler,
            MarketDataStore marketDataStore) {

        this.browserHandler = browserHandler;
        this.marketDataStore = marketDataStore;

        /*
         * Indexes manually because ye NSE_EQ nahi hain.
         */

        symbols.put(
                "NSE_INDEX|Nifty 50",
                "NIFTY 50"
        );

        symbols.put(
                "NSE_INDEX|Nifty Bank",
                "NIFTY BANK"
        );
    }

    // =========================================================
    // START
    // =========================================================

    @PostConstruct
    public void startMarketFeed() {

        try {

            if (accessToken == null
                    || accessToken.isBlank()
                    || accessToken.startsWith("${")) {

                System.err.println(
                        "UPSTOX ACCESS TOKEN NOT CONFIGURED"
                );

                return;
            }

            // =================================================
            // LOAD ALL NSE STOCKS
            // =================================================

            loadAllNseStocks();

            if (symbols.size() <= 2) {

                System.err.println(
                        "NO NSE EQUITY INSTRUMENTS LOADED"
                );

                return;
            }

            System.out.println(
                    "TOTAL NSE INSTRUMENTS LOADED = "
                            + (symbols.size() - 2)
            );

            System.out.println(
                    "TOTAL INCLUDING INDEXES = "
                            + symbols.size()
            );

            // =================================================
            // START UPSTOX
            // =================================================

            connect();

            // =================================================
            // START BROWSER DATA SENDER
            // =================================================

            startBrowserSender();

        } catch (Exception e) {

            System.err.println(
                    "FAILED TO START UPSTOX MARKET FEED"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // LOAD ALL NSE EQUITY STOCKS
    // =========================================================

    private void loadAllNseStocks() {

        String instrumentUrl =
                "https://assets.upstox.com/market-quote/instruments/exchange/complete.json.gz";

        HttpURLConnection connection = null;

        try {

            System.out.println(
                    "DOWNLOADING UPSTOX INSTRUMENT MASTER"
            );

            URL url =
                    URI.create(instrumentUrl).toURL();

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setConnectTimeout(15000);

            connection.setReadTimeout(60000);

            connection.setRequestProperty(
                    "Accept-Encoding",
                    "gzip"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.err.println(
                        "INSTRUMENT MASTER HTTP ERROR = "
                                + responseCode
                );

                return;
            }

            try (
                    InputStream inputStream =
                            connection.getInputStream();

                    GZIPInputStream gzipInputStream =
                            new GZIPInputStream(inputStream)
            ) {

                ObjectMapper objectMapper =
                        new ObjectMapper();

                JsonNode instruments =
                        objectMapper.readTree(
                                gzipInputStream
                        );

                int stockCount = 0;

                for (JsonNode instrument : instruments) {

                    String segment =
                            instrument
                                    .path("segment")
                                    .asText();

                    String instrumentType =
                            instrument
                                    .path("instrument_type")
                                    .asText();

                    String instrumentKey =
                            instrument
                                    .path("instrument_key")
                                    .asText();

                    String tradingSymbol =
                            instrument
                                    .path("trading_symbol")
                                    .asText();

                    /*
                     * ONLY NSE CASH EQUITY
                     */

                    if (
                            "NSE_EQ".equals(segment)
                                    &&
                            "EQ".equals(instrumentType)
                                    &&
                            instrumentKey != null
                                    &&
                            !instrumentKey.isBlank()
                                    &&
                            tradingSymbol != null
                                    &&
                            !tradingSymbol.isBlank()
                    ) {

                        symbols.put(
                                instrumentKey,
                                tradingSymbol
                        );

                        stockCount++;
                    }
                }

                System.out.println(
                        "NSE EQUITY STOCKS FOUND = "
                                + stockCount
                );
            }

            System.out.println(
                    "TOTAL INSTRUMENTS INCLUDING INDEXES = "
                            + symbols.size()
            );

            System.out.println(
                    "========================================"
            );

        } catch (Exception e) {

            System.err.println(
                    "FAILED TO LOAD INSTRUMENT MASTER"
            );

            e.printStackTrace();

        } finally {

            if (connection != null) {

                connection.disconnect();
            }
        }
    }

    // =========================================================
    // CONNECT UPSTOX
    // =========================================================

    private void connect() throws ApiException {

        ApiClient client =
                com.upstox.Configuration
                        .getDefaultApiClient();

        OAuth oauth =
                (OAuth) client
                        .getAuthentication("OAUTH2");

        oauth.setAccessToken(accessToken);

        // =====================================================
        // SUBSCRIPTION KEYS
        // =====================================================

        Set<String> fullSubscriptionKeys =
                new LinkedHashSet<>();

        /*
         * First NIFTY indexes
         */

        fullSubscriptionKeys.add(
                "NSE_INDEX|Nifty 50"
        );

        fullSubscriptionKeys.add(
                "NSE_INDEX|Nifty Bank"
        );

        /*
         * Remaining slots stocks ko denge.
         */

        for (String instrumentKey : symbols.keySet()) {

            if (
                    fullSubscriptionKeys.size()
                            >= MAX_FULL_INSTRUMENTS
            ) {
                break;
            }

            /*
             * Index duplicate skip
             */

            if (
                    instrumentKey.equals(
                            "NSE_INDEX|Nifty 50"
                    )
            ) {
                continue;
            }

            if (
                    instrumentKey.equals(
                            "NSE_INDEX|Nifty Bank"
                    )
            ) {
                continue;
            }

            fullSubscriptionKeys.add(
                    instrumentKey
            );
        }

        System.out.println(
                "TOTAL LOADED INSTRUMENTS = "
                        + symbols.size()
        );

        System.out.println(
                "FULL SUBSCRIPTION = "
                        + fullSubscriptionKeys.size()
        );

        // =====================================================
        // MARKET DATA STREAMER V3
        // =====================================================

        streamer =
                new MarketDataStreamerV3(
                        client,
                        fullSubscriptionKeys,
                        Mode.FULL
                );

        // =====================================================
        // ON OPEN
        // =====================================================

        streamer.setOnOpenListener(
                new OnOpenListener() {

                    @Override
                    public void onOpen() {

                        System.out.println(
                                "UPSTOX WEBSOCKET CONNECTED"
                        );

                        System.out.println(
                                "FULL INSTRUMENTS = "
                                        + fullSubscriptionKeys.size()
                        );

                        System.out.println(
                                "FULL MODE ENABLED"
                        );

                        System.out.println(
                                "5 LEVEL MARKET DEPTH ENABLED"
                        );

                        System.out.println(
                                "OHLC ENABLED"
                        );

                        System.out.println(
                                "TOTAL BUY / SELL ENABLED"
                        );
                    }
                }
        );

        // =====================================================
        // MARKET UPDATE
        // =====================================================

        streamer.setOnMarketUpdateListener(

                new OnMarketUpdateV3Listener() {

                    @Override
                    public void onUpdate(
                            MarketUpdateV3 update) {

                        processUpdate(update);
                    }
                }
        );

        // =====================================================
        // CONNECT
        // =====================================================

        streamer.connect();
    }

    // =========================================================
    // PROCESS UPSTOX UPDATE
    // =========================================================

    private void processUpdate(
            MarketUpdateV3 update) {

        try {

            if (
                    update == null
                            ||
                    update.getFeeds() == null
                            ||
                    update.getFeeds().isEmpty()
            ) {

                return;
            }

            update.getFeeds().forEach(
                    (instrumentKey, feed) -> {

                        try {

                            String symbol =
                                    symbols.getOrDefault(
                                            instrumentKey,
                                            instrumentKey
                                    );

                            double ltp = 0;

                            double previousClose = 0;

                            double open = 0;

                            double high = 0;

                            double low = 0;

                            long volume = 0;

                            long lastQuantity = 0;

                            long totalBuyQuantity = 0;

                            long totalSellQuantity = 0;

                            List<DepthLevel> bidLevels =
                                    new ArrayList<>();

                            List<DepthLevel> askLevels =
                                    new ArrayList<>();

                            // =================================
                            // FULL FEED
                            // =================================

                            if (
                                    feed.getFullFeed()
                                            != null
                            ) {

                                // =================================
                                // INDEX FULL FEED
                                // =================================

                                if (
                                        feed.getFullFeed()
                                                .getIndexFF()
                                                != null
                                ) {

                                    var indexFF =
                                            feed.getFullFeed()
                                                    .getIndexFF();

                                    // -----------------------------
                                    // LTPC
                                    // -----------------------------

                                    if (
                                            indexFF.getLtpc()
                                                    != null
                                    ) {

                                        var ltpc =
                                                indexFF.getLtpc();

                                        ltp =
                                                ltpc.getLtp();

                                        previousClose =
                                                ltpc.getCp();

                                        lastQuantity =
                                                ltpc.getLtq();
                                    }

                                    // -----------------------------
                                    // OHLC
                                    // -----------------------------

                                    if (
                                            indexFF
                                                    .getMarketOHLC()
                                                    != null
                                    ) {

                                        var ohlcList =
                                                indexFF
                                                        .getMarketOHLC()
                                                        .getOhlc();

                                        if (
                                                ohlcList != null
                                                        &&
                                                !ohlcList.isEmpty()
                                        ) {

                                            var daily =
                                                    ohlcList
                                                            .stream()
                                                            .filter(
                                                                    o ->
                                                                            "1d".equals(
                                                                                    o.getInterval()
                                                                            )
                                                            )
                                                            .findFirst()
                                                            .orElse(
                                                                    ohlcList.get(0)
                                                            );

                                            open =
                                                    daily.getOpen();

                                            high =
                                                    daily.getHigh();

                                            low =
                                                    daily.getLow();

                                            volume =
                                                    daily.getVol();
                                        }
                                    }
                                }

                                // =================================
                                // MARKET FULL FEED
                                // =================================

                                if (
                                        feed.getFullFeed()
                                                .getMarketFF()
                                                != null
                                ) {

                                    var marketFF =
                                            feed.getFullFeed()
                                                    .getMarketFF();

                                    // -----------------------------
                                    // LTPC
                                    // -----------------------------

                                    if (
                                            marketFF.getLtpc()
                                                    != null
                                    ) {

                                        var ltpc =
                                                marketFF.getLtpc();

                                        ltp =
                                                ltpc.getLtp();

                                        previousClose =
                                                ltpc.getCp();

                                        lastQuantity =
                                                ltpc.getLtq();
                                    }

                                    // -----------------------------
                                    // OHLC
                                    // -----------------------------

                                    if (
                                            marketFF
                                                    .getMarketOHLC()
                                                    != null
                                    ) {

                                        var ohlcList =
                                                marketFF
                                                        .getMarketOHLC()
                                                        .getOhlc();

                                        if (
                                                ohlcList != null
                                                        &&
                                                !ohlcList.isEmpty()
                                        ) {

                                            var daily =
                                                    ohlcList
                                                            .stream()
                                                            .filter(
                                                                    o ->
                                                                            "1d".equals(
                                                                                    o.getInterval()
                                                                            )
                                                            )
                                                            .findFirst()
                                                            .orElse(
                                                                    ohlcList.get(0)
                                                            );

                                            open =
                                                    daily.getOpen();

                                            high =
                                                    daily.getHigh();

                                            low =
                                                    daily.getLow();

                                            volume =
                                                    daily.getVol();
                                        }
                                    }

                                    // -----------------------------
                                    // TOTAL BUY / SELL
                                    // -----------------------------

                                    totalBuyQuantity =
                                            (long) marketFF.getTbq();

                                    totalSellQuantity =
                                            (long) marketFF.getTsq();

                                    // -----------------------------
                                    // MARKET DEPTH
                                    // -----------------------------

                                    if (
                                            marketFF.getMarketLevel()
                                                    != null
                                                    &&
                                            marketFF
                                                    .getMarketLevel()
                                                    .getBidAskQuote()
                                                    != null
                                    ) {

                                        var quotes =
                                                marketFF
                                                        .getMarketLevel()
                                                        .getBidAskQuote();

                                        for (
                                                var quote : quotes
                                        ) {

                                            // BID
                                            if (
                                                    quote.getBidP()
                                                            != 0
                                                            ||
                                                    quote.getBidQ()
                                                            != 0
                                            ) {

                                                bidLevels.add(
                                                        new DepthLevel(
                                                                quote.getBidP(),
                                                                quote.getBidQ()
                                                        )
                                                );
                                            }

                                            // ASK
                                            if (
                                                    quote.getAskP()
                                                            != 0
                                                            ||
                                                    quote.getAskQ()
                                                            != 0
                                            ) {

                                                askLevels.add(
                                                        new DepthLevel(
                                                                quote.getAskP(),
                                                                quote.getAskQ()
                                                        )
                                                );
                                            }
                                        }
                                    }
                                }
                            }

                            // =================================
                            // CHANGE
                            // =================================

                            double change =
                                    ltp - previousClose;

                            double changePercent = 0;

                            if (previousClose != 0) {

                                changePercent =
                                        (change
                                                / previousClose)
                                                * 100;
                            }

                            // =================================
                            // CREATE MARKET TICK
                            // =================================

                            MarketTick tick =
                                    new MarketTick(

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

                                            totalBuyQuantity,

                                            totalSellQuantity,

                                            String.valueOf(
                                                    System.currentTimeMillis()
                                            ),

                                            bidLevels,

                                            askLevels
                                    );

                            // =================================
                            // STORE LATEST TICK
                            // =================================

                            latestTicks.put(
                                    instrumentKey,
                                    tick
                            );

                            /*
                             * Mark this instrument as updated.
                             *
                             * Browser sender next cycle me
                             * iska latest tick bhej dega.
                             */

                            dirtyInstruments.add(
                                    instrumentKey
                            );

                            // =================================
                            // EXISTING STORE
                            // =================================

                            marketDataStore.update(
                                    tick
                            );

                        } catch (Exception e) {

                            System.err.println(
                                    "ERROR PROCESSING "
                                            + instrumentKey
                            );

                            e.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {

            System.err.println(
                    "ERROR PROCESSING UPSTOX UPDATE"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // START BROWSER SENDER
    // =========================================================

    private void startBrowserSender() {

        System.out.println(
                "STARTING BROWSER MARKET DATA SENDER"
        );

        browserSender.scheduleAtFixedRate(

                this::sendLatestDataToBrowser,

                0,

                BROWSER_SEND_INTERVAL_MS,

                TimeUnit.MILLISECONDS
        );
    }

    // =========================================================
    // SEND LATEST DATA TO BROWSER
    // =========================================================

    private void sendLatestDataToBrowser() {

        try {

            if (dirtyInstruments.isEmpty()) {
                return;
            }

            /*
             * Current dirty instruments ka snapshot.
             *
             * Upstox meanwhile naye updates receive kar sakta hai.
             */

            List<String> instrumentsToSend =
                    new ArrayList<>(
                            dirtyInstruments
                    );

            for (
                    String instrumentKey :
                    instrumentsToSend
            ) {

                /*
                 * Remove before sending.
                 *
                 * Agar meanwhile new update aaya
                 * to processUpdate() dobara add karega.
                 */

                dirtyInstruments.remove(
                        instrumentKey
                );

                MarketTick tick =
                        latestTicks.get(
                                instrumentKey
                        );

                if (tick == null) {
                    continue;
                }

                /*
                 * IMPORTANT:
                 *
                 * Ab browser WebSocket directly
                 * Upstox callback ke andar nahi hai.
                 */

                browserHandler.sendMarketData(
                        tick
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "BROWSER MARKET DATA SENDER ERROR"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    @PreDestroy
    public void stopMarketFeed() {

        System.out.println(
                "STOPPING UPSTOX MARKET SERVICE"
        );

        // =============================================
        // STOP BROWSER SENDER
        // =============================================

        try {

            browserSender.shutdownNow();

            System.out.println(
                    "BROWSER MARKET DATA SENDER STOPPED"
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

        // =============================================
        // STOP UPSTOX
        // =============================================

        try {

            if (streamer != null) {

                streamer.disconnect();

                System.out.println(
                        "UPSTOX WEBSOCKET DISCONNECTED"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}