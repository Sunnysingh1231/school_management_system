
package com.example.demo.service;

import com.example.demo.model.EmaTrade;
import com.example.demo.repository.EmaTradeRepository;
import com.example.demo.webSocket.EmaBuySellHandler;

import tools.jackson.databind.ObjectMapper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmaBuySellFilterService {

    private static final double BUY_STRENGTH = 100.0;

    private final LiveEmaCalculatorService emaCalculatorService;

    private final EmaTradeRepository emaTradeRepository;

    /*
     * Spring Boot ka ObjectMapper inject hoga.
     * Isme Java 8 date/time support available rahega.
     */
    private final ObjectMapper objectMapper;

    private final EmaBuySellHandler emaBuySellHandler;


    /*
     * ================================================
     * ACTIVE BUY POSITIONS
     * ================================================
     *
     * instrumentKey -> EmaTrade
     */
    private final Map<String, EmaTrade> activeTrades =
            new ConcurrentHashMap<>();


    public EmaBuySellFilterService(
            LiveEmaCalculatorService emaCalculatorService,
            EmaTradeRepository emaTradeRepository,
            EmaBuySellHandler emaBuySellHandler,
            ObjectMapper objectMapper) {

        this.emaCalculatorService = emaCalculatorService;

        this.emaTradeRepository = emaTradeRepository;

        this.emaBuySellHandler = emaBuySellHandler;

        this.objectMapper = objectMapper;
    }


    /*
     * ================================================
     * RUN EVERY 500 MILLISECOND
     * ================================================
     */

    @Scheduled(fixedRate = 500)
    public void processAllStocks() {

        Map<String, LiveEmaCalculatorService.TrendResult> results =
                emaCalculatorService.getLatestResults();

        System.out.println(
                "EMA FILTER STOCK COUNT : "
                        + results.size()
        );

        if (results.isEmpty()) {

            System.out.println(
                    "EMA FILTER : NO DATA"
            );

            return;
        }

        for (LiveEmaCalculatorService.TrendResult result
                : results.values()) {

            processEmaData(result);
        }
    }


    /*
     * ================================================
     * PROCESS EMA DATA
     * ================================================
     */

    public void processEmaData(
            LiveEmaCalculatorService.TrendResult result) {

        String instrumentKey =
                result.getInstrumentKey();

        double bullishStrength =
                result.getBullishStrength();


        System.out.println(
                "EMA CHECK : "
                        + instrumentKey
                        + " | PRICE : "
                        + result.getCurrentPrice()
                        + " | BULLISH : "
                        + bullishStrength
        );


        /*
         * ============================================
         * BUY
         * ============================================
         */

        if (bullishStrength == BUY_STRENGTH) {

            buyIfNotAlreadyBought(result);

            return;
        }


        /*
         * ============================================
         * SELL
         * ============================================
         */

        if (bullishStrength < BUY_STRENGTH) {

            sellIfAlreadyBought(result);
        }
    }


    /*
     * ================================================
     * BUY
     * ================================================
     */

    private void buyIfNotAlreadyBought(
            LiveEmaCalculatorService.TrendResult result) {

        String instrumentKey =
                result.getInstrumentKey();


        /*
         * Already BUY hai
         */
        if (activeTrades.containsKey(instrumentKey)) {

            return;
        }


        /*
         * ============================================
         * CREATE TRADE
         * ============================================
         */

        EmaTrade trade = new EmaTrade();

        trade.setInstrumentKey(
                instrumentKey
        );

        trade.setBuyPrice(
                result.getCurrentPrice()
        );

        trade.setBullishStrength(
                result.getBullishStrength()
        );

        trade.setBearishStrength(
                result.getBearishStrength()
        );

        trade.setEma20(
                result.getEma20()
        );

        trade.setEma50(
                result.getEma50()
        );

        trade.setEma200(
                result.getEma200()
        );

        trade.setTrend(
                result.getTrend().name()
        );

        trade.setStatus(
                "BUY"
        );

        trade.setBuyTime(
                LocalDateTime.now()
        );


        /*
         * ============================================
         * MYSQL SAVE
         * ============================================
         */

        trade = emaTradeRepository.save(trade);


        /*
         * ============================================
         * ACTIVE MEMORY
         * ============================================
         */

        activeTrades.put(
                instrumentKey,
                trade
        );


        /*
         * ============================================
         * CONVERT TO JSON
         * ============================================
         */

        try {

            String json =
                    objectMapper.writeValueAsString(trade);


            /*
             * ========================================
             * SEND BUY JSON TO WEBSOCKET
             * ========================================
             */

            emaBuySellHandler.sendMessage(json);


            System.out.println(
                    "EMA BUY JSON SENT : "
                            + json
            );

        } catch (Exception e) {

            System.out.println(
                    "EMA BUY JSON ERROR : "
            );

            e.printStackTrace();
        }
    }


    /*
     * ================================================
     * SELL
     * ================================================
     */

    private void sellIfAlreadyBought(
            LiveEmaCalculatorService.TrendResult result) {

        String instrumentKey =
                result.getInstrumentKey();


        /*
         * ============================================
         * FIND ACTIVE BUY
         * ============================================
         */

        EmaTrade trade =
                activeTrades.get(instrumentKey);


        /*
         * BUY nahi hua hai
         */
        if (trade == null) {

            return;
        }


        /*
         * ============================================
         * BUY / SELL PRICE
         * ============================================
         */

        double buyPrice =
                trade.getBuyPrice();

        double sellPrice =
                result.getCurrentPrice();


        /*
         * ============================================
         * CHANGE %
         * ============================================
         */

        double changePercentage = 0.0;

        if (buyPrice != 0) {

            changePercentage =
                    ((sellPrice - buyPrice)
                            / buyPrice)
                            * 100.0;
        }


        /*
         * ============================================
         * PROFIT / LOSS
         * ============================================
         */

        double profitLoss =
                sellPrice - buyPrice;


        /*
         * ============================================
         * UPDATE TRADE
         * ============================================
         */

        trade.setSellPrice(
                sellPrice
        );

        trade.setChangePercentage(
                changePercentage
        );

        trade.setProfitLoss(
                profitLoss
        );

        trade.setBullishStrength(
                result.getBullishStrength()
        );

        trade.setBearishStrength(
                result.getBearishStrength()
        );

        trade.setEma20(
                result.getEma20()
        );

        trade.setEma50(
                result.getEma50()
        );

        trade.setEma200(
                result.getEma200()
        );

        trade.setTrend(
                result.getTrend().name()
        );

        trade.setStatus(
                "SELL"
        );

        trade.setSellTime(
                LocalDateTime.now()
        );


        /*
         * ============================================
         * MYSQL UPDATE
         * ============================================
         */

        trade = emaTradeRepository.save(trade);


        /*
         * ============================================
         * REMOVE ACTIVE BUY
         * ============================================
         */

        activeTrades.remove(
                instrumentKey
        );


        /*
         * ============================================
         * CONVERT SELL TO JSON
         * ============================================
         */

        try {

            String json =
                    objectMapper.writeValueAsString(trade);


            /*
             * ========================================
             * SEND SELL JSON TO WEBSOCKET
             * ========================================
             */

            emaBuySellHandler.sendMessage(json);


            System.out.println(
                    "EMA SELL JSON SENT : "
                            + json
            );

        } catch (Exception e) {

            System.out.println(
                    "EMA SELL JSON ERROR : "
            );

            e.printStackTrace();
        }
    }
}