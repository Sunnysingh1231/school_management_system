package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.model.MarketTick;
import com.example.demo.webSocket.HistoricalLiveWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HistoricalLiveSimulator {

    private final Nifty50FiveMinuteHistoricalService historicalService;

    private final HistoricalLiveWebSocketHandler webSocketHandler;
    // =========================================
    // ALL HISTORICAL DATA
    // =========================================

    private final Map<String, List<Candle>> historicalData =
            new ConcurrentHashMap<>();

    // =========================================
    // CURRENT MARKET TICK OF EACH STOCK
    // =========================================

    private final Map<String, MarketTick> liveStocks =
            new ConcurrentHashMap<>();

    // =========================================
    // CURRENT CANDLE INDEX OF EACH STOCK
    // =========================================

    private final Map<String, AtomicInteger> candleIndexes =
            new ConcurrentHashMap<>();

    // =========================================
    // TIMER
    // =========================================

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = false;


    public HistoricalLiveSimulator(
            Nifty50FiveMinuteHistoricalService historicalService, HistoricalLiveWebSocketHandler webSocketHandler) {

        this.historicalService = historicalService;
		this.webSocketHandler = webSocketHandler;
    }


    // =========================================
    // APPLICATION START
    // =========================================

    @PostConstruct
    public void initialize() {

        System.out.println();
        System.out.println("========================================");
        System.out.println("HISTORICAL LIVE SIMULATOR");
        System.out.println("========================================");

        /*
         * Yaha apni stocks ki list do.
         *
         * Baad mein isi ko ALL NSE stocks
         * ke dynamic instrument list se replace
         * kar sakte hain.
         */

        List<String> stocks = List.of(

                "NSE_EQ|INE040A01034"

                // aur stocks:
                // "NSE_EQ|....",
                // "NSE_EQ|...."
        );

        loadHistoricalData(stocks);
    }


    // =========================================
    // LOAD DATA ONLY ONCE
    // =========================================

    private void loadHistoricalData(
            List<String> instrumentKeys) {

        System.out.println("Loading historical data...");

        for (String instrumentKey : instrumentKeys) {

            try {

                /*
                 * IMPORTANT:
                 *
                 * API yaha sirf EK BAAR call hogi.
                 */

                List<Candle> candles =
                        historicalService
                                .getPrevious20Days5Minute(
                                        instrumentKey
                                );

                if (candles == null || candles.isEmpty()) {

                    System.out.println(
                            "NO DATA : " + instrumentKey
                    );

                    continue;
                }


                // =====================================
                // STORE ALL CANDLES IN MEMORY
                // =====================================

                historicalData.put(
                        instrumentKey,
                        new ArrayList<>(candles)
                );


                // =====================================
                // CREATE MARKET TICK
                // =====================================

                MarketTick tick = new MarketTick();

                tick.setInstrumentKey(instrumentKey);

                String symbol =
                        instrumentKey.substring(
                                instrumentKey.lastIndexOf("|") + 1
                        );

                tick.setSymbol(symbol);


                // Historical API mein ye available nahi
                tick.setLastQuantity(0);
                tick.setTotByQ(0);
                tick.setTotSlQ(0);

                tick.setBidLevels(null);
                tick.setAskLevels(null);


                liveStocks.put(
                        instrumentKey,
                        tick
                );


                // =====================================
                // START FROM FIRST CANDLE
                // =====================================

                candleIndexes.put(
                        instrumentKey,
                        new AtomicInteger(0)
                );


                System.out.println(
                        "LOADED : "
                                + symbol
                                + " | CANDLES : "
                                + candles.size()
                );

            } catch (Exception e) {

                System.err.println(
                        "ERROR : "
                                + instrumentKey
                );

                e.printStackTrace();
            }
        }


        if (historicalData.isEmpty()) {

            System.out.println(
                    "NO HISTORICAL DATA FOUND"
            );

            return;
        }


        System.out.println();
        System.out.println(
                "Historical data loaded successfully."
        );


        // =========================================
        // NOW START 5 SECOND SIMULATION
        // =========================================

        running = true;

        scheduler.scheduleAtFixedRate(

                this::processNextCandle,

                0,

                5,

                TimeUnit.SECONDS
        );

        System.out.println(
                "5 SECOND LIVE SIMULATION STARTED"
        );
    }


    // =========================================
    // EVERY 5 SECOND
    // =========================================

    private void processNextCandle() {

        if (!running) {
            return;
        }


        for (Map.Entry<String, List<Candle>> entry :
                historicalData.entrySet()) {


            String instrumentKey =
                    entry.getKey();

            List<Candle> candles =
                    entry.getValue();


            MarketTick tick =
                    liveStocks.get(
                            instrumentKey
                    );


            AtomicInteger counter =
                    candleIndexes.get(
                            instrumentKey
                    );


            if (tick == null || counter == null) {
                continue;
            }


            // =====================================
            // GET NEXT CANDLE FROM MEMORY
            // =====================================

            int index =
                    counter.getAndIncrement();


            // =====================================
            // DATA FINISHED
            // =====================================

            if (index >= candles.size()) {

                System.out.println(
                        "DATA FINISHED : "
                                + tick.getSymbol()
                );

                continue;
            }


            Candle candle =
                    candles.get(index);


            // =====================================
            // UPDATE MARKET TICK
            // =====================================

            tick.setOpen(
                    candle.getOpen()
            );

            tick.setHigh(
                    candle.getHigh()
            );

            tick.setLow(
                    candle.getLow()
            );

            tick.setLtp(
                    candle.getClose()
            );

            tick.setVolume(
                    candle.getVolume()
            );

            tick.setUpdateTime(
                    candle.getTimestamp().toString()
            );


            // =====================================
            // CHANGE
            // =====================================

            if (index > 0) {

                double previousClose =
                        candles
                                .get(index - 1)
                                .getClose();


                double currentClose =
                        candle.getClose();


                double change =
                        currentClose - previousClose;


                double changePercent =
                        previousClose != 0
                                ? (change / previousClose) * 100
                                : 0;


                tick.setPreviousClose(
                        previousClose
                );

                tick.setChange(
                        change
                );

                tick.setChangePercent(
                        changePercent
                );

            } else {

                tick.setPreviousClose(
                        candle.getClose()
                );

                tick.setChange(0);

                tick.setChangePercent(0);
            }


            // =====================================
            // PRINT
            // =====================================

            System.out.println();

            System.out.println(
                    "========== LIVE =========="
            );

            System.out.println(
                    "STOCK      : "
                            + tick.getSymbol()
            );

            System.out.println(
                    "TIME       : "
                            + tick.getUpdateTime()
            );

            System.out.println(
                    "OPEN       : "
                            + tick.getOpen()
            );

            System.out.println(
                    "HIGH       : "
                            + tick.getHigh()
            );

            System.out.println(
                    "LOW        : "
                            + tick.getLow()
            );

            System.out.println(
                    "LTP        : "
                            + tick.getLtp()
            );

            System.out.println(
                    "VOLUME     : "
                            + tick.getVolume()
            );

            System.out.println(
                    "CHANGE     : "
                            + tick.getChange()
            );

            System.out.println(
                    "CHANGE %   : "
                            + tick.getChangePercent()
            );

            System.out.println(
                    "LAST QTY   : NOT AVAILABLE"
            );

            System.out.println(
                    "TOTAL BID  : NOT AVAILABLE"
            );

            System.out.println(
                    "TOTAL ASK  : NOT AVAILABLE"
            );

            System.out.println(
                    "DEPTH      : NOT AVAILABLE"
            );

            System.out.println(
                    "CANDLE     : "
                            + (index + 1)
                            + "/"
                            + candles.size()
            );

            System.out.println(
                    "=========================="
            );
            
            webSocketHandler.sendMarketData(tick);
            
        }
    }


    // =========================================
    // GET ALL CURRENT STOCK DATA
    // =========================================

    public Map<String, MarketTick> getLiveStocks() {

        return liveStocks;
    }


    // =========================================
    // GET ONE STOCK
    // =========================================

    public MarketTick getStock(
            String instrumentKey) {

        return liveStocks.get(
                instrumentKey
        );
    }


    // =========================================
    // STOP
    // =========================================

    public void stopSimulation() {

        running = false;

        scheduler.shutdownNow();

        System.out.println(
                "Historical simulation stopped."
        );
    }


    // =========================================
    // SHUTDOWN
    // =========================================

    @PreDestroy
    public void shutdown() {

        running = false;

        scheduler.shutdownNow();
    }
}