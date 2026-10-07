package com.example.demo.service;

import com.example.demo.model.Candle;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TrendFilterService {

    private final Nifty50FiveMinuteHistoricalService historicalService;
    private final UpstoxMarketService upstoxMarketService;

    public TrendFilterService(
            Nifty50FiveMinuteHistoricalService historicalService,
            UpstoxMarketService upstoxMarketService) {

        this.historicalService = historicalService;
        this.upstoxMarketService = upstoxMarketService;
    }

    // =====================================================
    // EMA PERIODS
    // =====================================================

    private static final int EMA_SHORT = 20;
    private static final int EMA_MEDIUM = 50;
    private static final int EMA_LONG = 200;

    // =====================================================
    // TREND TYPE
    // =====================================================

    public enum TrendType {

        STRONG_BULLISH,
        BULLISH,
        SIDEWAYS,
        BEARISH,
        STRONG_BEARISH,
        INSUFFICIENT_DATA
    }

    // =====================================================
    // TREND RESULT
    // =====================================================

    public static class TrendResult {

        private final String instrumentKey;

        private final double currentPrice;

        private final double ema20;
        private final double ema50;
        private final double ema200;

        private final TrendType trend;

        private final double bullishStrength;
        private final double bearishStrength;

        private final boolean priceAboveEma20;
        private final boolean priceAboveEma50;
        private final boolean priceAboveEma200;

        private final boolean bullishAlignment;
        private final boolean bearishAlignment;

        public TrendResult(
                String instrumentKey,
                double currentPrice,
                double ema20,
                double ema50,
                double ema200,
                TrendType trend,
                double bullishStrength,
                double bearishStrength,
                boolean priceAboveEma20,
                boolean priceAboveEma50,
                boolean priceAboveEma200,
                boolean bullishAlignment,
                boolean bearishAlignment) {

            this.instrumentKey = instrumentKey;
            this.currentPrice = currentPrice;
            this.ema20 = ema20;
            this.ema50 = ema50;
            this.ema200 = ema200;
            this.trend = trend;
            this.bullishStrength = bullishStrength;
            this.bearishStrength = bearishStrength;
            this.priceAboveEma20 = priceAboveEma20;
            this.priceAboveEma50 = priceAboveEma50;
            this.priceAboveEma200 = priceAboveEma200;
            this.bullishAlignment = bullishAlignment;
            this.bearishAlignment = bearishAlignment;
        }

        public String getInstrumentKey() {
            return instrumentKey;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public double getEma20() {
            return ema20;
        }

        public double getEma50() {
            return ema50;
        }

        public double getEma200() {
            return ema200;
        }

        public TrendType getTrend() {
            return trend;
        }

        public double getBullishStrength() {
            return bullishStrength;
        }

        public double getBearishStrength() {
            return bearishStrength;
        }

        public boolean isPriceAboveEma20() {
            return priceAboveEma20;
        }

        public boolean isPriceAboveEma50() {
            return priceAboveEma50;
        }

        public boolean isPriceAboveEma200() {
            return priceAboveEma200;
        }

        public boolean isBullishAlignment() {
            return bullishAlignment;
        }

        public boolean isBearishAlignment() {
            return bearishAlignment;
        }
    }

    // =====================================================
    // ANALYZE ONE STOCK
    // =====================================================

    public TrendResult analyzeTrend(String instrumentKey)
            throws Exception {

        List<Candle> candles =
                historicalService.getPrevious20Days5Minute(
                        instrumentKey
                );

        if (candles == null || candles.size() < EMA_LONG) {

            return new TrendResult(
                    instrumentKey,
                    0,
                    0,
                    0,
                    0,
                    TrendType.INSUFFICIENT_DATA,
                    0,
                    0,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        // =================================================
        // CLOSE PRICES
        // =================================================

        List<Double> closes = new ArrayList<>();

        for (Candle candle : candles) {
            closes.add(candle.getClose());
        }

        // =================================================
        // EMA
        // =================================================

        double ema20 = calculateEMA(
                closes,
                EMA_SHORT
        );

        double ema50 = calculateEMA(
                closes,
                EMA_MEDIUM
        );

        double ema200 = calculateEMA(
                closes,
                EMA_LONG
        );

        double currentPrice =
                closes.get(closes.size() - 1);

        // =================================================
        // PRICE VS EMA
        // =================================================

        boolean above20 =
                currentPrice > ema20;

        boolean above50 =
                currentPrice > ema50;

        boolean above200 =
                currentPrice > ema200;

        // =================================================
        // EMA ALIGNMENT
        // =================================================

        boolean bullishAlignment =
                ema20 > ema50 &&
                ema50 > ema200;

        boolean bearishAlignment =
                ema20 < ema50 &&
                ema50 < ema200;

        // =================================================
        // BULLISH / BEARISH STRENGTH
        // =================================================

        double bullishStrength = 0;
        double bearishStrength = 0;

        if (above20) {
            bullishStrength += 20;
        }

        if (above50) {
            bullishStrength += 20;
        }

        if (above200) {
            bullishStrength += 20;
        }

        if (bullishAlignment) {
            bullishStrength += 40;
        }

        if (!above20) {
            bearishStrength += 20;
        }

        if (!above50) {
            bearishStrength += 20;
        }

        if (!above200) {
            bearishStrength += 20;
        }

        if (bearishAlignment) {
            bearishStrength += 40;
        }

        // =================================================
        // TREND CLASSIFICATION
        // =================================================

        TrendType trend;

        if (bullishStrength >= 80) {

            trend = TrendType.STRONG_BULLISH;

        } else if (bullishStrength >= 60) {

            trend = TrendType.BULLISH;

        } else if (bearishStrength >= 80) {

            trend = TrendType.STRONG_BEARISH;

        } else if (bearishStrength >= 60) {

            trend = TrendType.BEARISH;

        } else {

            trend = TrendType.SIDEWAYS;
        }

        // =================================================
        // RESULT
        // =================================================

        return new TrendResult(
                instrumentKey,
                currentPrice,
                ema20,
                ema50,
                ema200,
                trend,
                bullishStrength,
                bearishStrength,
                above20,
                above50,
                above200,
                bullishAlignment,
                bearishAlignment
        );
    }

    // =====================================================
    // EMA CALCULATION
    // =====================================================

    private double calculateEMA(
            List<Double> prices,
            int period) {

        if (prices.size() < period) {
            return 0;
        }

        // Initial SMA
        double sum = 0;

        for (int i = 0; i < period; i++) {
            sum += prices.get(i);
        }

        double ema = sum / period;

        // EMA multiplier
        double multiplier =
                2.0 / (period + 1);

        // Remaining candles
        for (int i = period; i < prices.size(); i++) {

            double price = prices.get(i);

            ema =
                    (price - ema) * multiplier
                            + ema;
        }

        return ema;
    }

    // =====================================================
    // CHECK ONE STOCK
    // =====================================================

    public void checkStockTrend(
            String instrumentKey,
            String symbol)
            throws Exception {

        TrendResult result =
                analyzeTrend(instrumentKey);

//        System.out.println();
//        System.out.println(
//                "===================================================="
//        );
//
//        System.out.println(
//                "STOCK : " + symbol
//        );
//
//        System.out.println(
//                "KEY   : " + instrumentKey
//        );
//
//        System.out.println(
//                "PRICE : " + result.getCurrentPrice()
//        );
//
//        System.out.println(
//                "EMA20 : " + result.getEma20()
//        );
//
//        System.out.println(
//                "EMA50 : " + result.getEma50()
//        );
//
//        System.out.println(
//                "EMA200: " + result.getEma200()
//        );
//
//        System.out.println(
//                "TREND : " + result.getTrend()
//        );
//
//        System.out.println(
//                "BULLISH STRENGTH : "
//                        + result.getBullishStrength()
//                        + "%"
//        );
//
//        System.out.println(
//                "BEARISH STRENGTH : "
//                        + result.getBearishStrength()
//                        + "%"
//        );
//
//        System.out.println(
//                "PRICE > EMA20  : "
//                        + result.isPriceAboveEma20()
//        );
//
//        System.out.println(
//                "PRICE > EMA50  : "
//                        + result.isPriceAboveEma50()
//        );
//
//        System.out.println(
//                "PRICE > EMA200 : "
//                        + result.isPriceAboveEma200()
//        );
//
//        System.out.println(
//                "BULLISH ALIGNMENT : "
//                        + result.isBullishAlignment()
//        );
//
//        System.out.println(
//                "BEARISH ALIGNMENT : "
//                        + result.isBearishAlignment()
//        );
//
//        System.out.println(
//                "===================================================="
//        );
    }

    // =====================================================
    // SCAN ALL NIFTY 50 STOCKS
    // =====================================================

    @PostConstruct
    public void testTrend() {

        System.out.println();
        System.out.println(
                "####################################################"
        );

        System.out.println(
                "STARTING NIFTY 50 EMA TREND FILTER"
        );

        System.out.println(
                "####################################################"
        );

        Map<String, String> stocks =
                upstoxMarketService.getNifty50Stocks();

        if (stocks == null || stocks.isEmpty()) {

            System.err.println(
                    "NIFTY 50 STOCK LIST IS EMPTY"
            );

            return;
        }

        System.out.println(
                "TOTAL STOCKS : " + stocks.size()
        );

        // =================================================
        // SCAN EVERY STOCK
        // =================================================

        for (Map.Entry<String, String> entry :
                stocks.entrySet()) {

            String instrumentKey =
                    entry.getKey();

            String symbol =
                    entry.getValue();

            try {

                checkStockTrend(
                        instrumentKey,
                        symbol
                );

            } catch (Exception e) {

                System.err.println(
                        "ERROR : "
                                + symbol
                                + " | "
                                + e.getMessage()
                );
            }
        }

        System.out.println();
        System.out.println(
                "####################################################"
        );

        System.out.println(
                "NIFTY 50 EMA TREND FILTER COMPLETED"
        );

        System.out.println(
                "####################################################"
        );
    }
}