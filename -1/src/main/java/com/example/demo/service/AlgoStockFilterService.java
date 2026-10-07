package com.example.demo.service;

import com.example.demo.model.AlgoStock;
import com.example.demo.model.Candle;
import com.example.demo.model.FilteredStock;
import com.example.demo.model.IndicatorResult;
import com.example.demo.model.MarketTick;
import com.example.demo.templates.MarketWebSocketHandler;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AlgoStockFilterService {

    private final MarketDataStore marketDataStore;
    private final CandleService candleService;
    private final IndicatorService indicatorService;
    private final MarketRegimeService marketRegimeService;
    private final LiquidityFilterService liquidityFilterService;
    private final TrendFilterService trendFilterService;
    private final MomentumFilterService momentumFilterService;
    private final VolumeVwapFilterService volumeVwapFilterService;
    private final AtrSetupFilterService atrSetupFilterService;
    private final EntryRiskService entryRiskService;
    private final MarketWebSocketHandler browserHandler;

    public AlgoStockFilterService(
            MarketDataStore marketDataStore,
            CandleService candleService,
            IndicatorService indicatorService,
            MarketRegimeService marketRegimeService,
            LiquidityFilterService liquidityFilterService,
            TrendFilterService trendFilterService,
            MomentumFilterService momentumFilterService,
            VolumeVwapFilterService volumeVwapFilterService,
            AtrSetupFilterService atrSetupFilterService,
            EntryRiskService entryRiskService,
            MarketWebSocketHandler browserHandler) {

        this.marketDataStore = marketDataStore;
        this.candleService = candleService;
        this.indicatorService = indicatorService;
        this.marketRegimeService = marketRegimeService;
        this.liquidityFilterService = liquidityFilterService;
        this.trendFilterService = trendFilterService;
        this.momentumFilterService = momentumFilterService;
        this.volumeVwapFilterService = volumeVwapFilterService;
        this.atrSetupFilterService = atrSetupFilterService;
        this.entryRiskService = entryRiskService;
        this.browserHandler = browserHandler;
    }

    public List<AlgoStock> scanStocks() {

        List<AlgoStock> finalStocks =
                new ArrayList<>();

        // =========================================================
        // STEP 1 - MARKET REGIME
        // =========================================================

        List<Candle> niftyDaily =
                candleService.getDailyCandles(
                        "NSE_INDEX|Nifty 50"
                );

        String regime =
                marketRegimeService
                        .filterMarketRegime(
                                niftyDaily
                        );

//        System.out.println(
//                "ALGO MARKET REGIME = " + regime
//        );

        if ("NO_DATA".equals(regime) ||
                "SIDEWAYS".equals(regime)) {

            return finalStocks;
        }

        // =========================================================
        // LIVE STOCKS
        // =========================================================

        List<MarketTick> stocks =
                marketDataStore.getAllStocks();

        for (MarketTick stock : stocks) {

            try {

                if (stock == null ||
                        stock.getInstrumentKey() == null ||
                        stock.getInstrumentKey()
                                .startsWith("NSE_INDEX")) {

                    continue;
                }

                FilteredStock filtered =
                        new FilteredStock();

                filtered.setInstrumentKey(
                        stock.getInstrumentKey()
                );

                filtered.setSymbol(
                        stock.getSymbol()
                );

                filtered.setLtp(
                        stock.getLtp()
                );

                filtered.setMarketRegime(
                        regime
                );

                // =================================================
                // STEP 2 - LIQUIDITY
                // =================================================

                boolean liquidity =
                        liquidityFilterService
                                .filterLiquidity(stock);

                filtered.setLiquidityPassed(
                        liquidity
                );

                filtered.setPassedStage(
                        liquidity ? 2 : 1
                );

                if (!liquidity) {

                    filtered.setFailedAt(
                            "LIQUIDITY"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // CANDLE DATA
                // =================================================

                List<Candle> daily =
                        candleService.getDailyCandles(
                                stock.getInstrumentKey()
                        );

                List<Candle> intraday =
                        candleService
                                .getIntraday15MinuteCandles(
                                        stock.getInstrumentKey()
                                );

                if (daily.size() < 200 ||
                        intraday.size() < 30) {

                    filtered.setFailedAt(
                            "CANDLE_DATA"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // INDICATORS
                // =================================================

                IndicatorResult indicators =
                        indicatorService.calculate(
                                daily,
                                intraday
                        );

                filtered.setEma20(
                        indicators.getEma20()
                );

                filtered.setEma50(
                        indicators.getEma50()
                );

                filtered.setEma200(
                        indicators.getEma200()
                );

                filtered.setRsi(
                        indicators.getRsi()
                );

                filtered.setMacd(
                        indicators.getMacd()
                );

                filtered.setMacdSignal(
                        indicators.getMacdSignal()
                );

                filtered.setRoc(
                        indicators.getRoc()
                );

                filtered.setVwap(
                        indicators.getVwap()
                );

                filtered.setAtr(
                        indicators.getAtr()
                );

                filtered.setVolumeRatio(
                        indicators.getVolumeRatio()
                );

                // =================================================
                // STEP 3 - TREND
                // =================================================

                boolean trend =
                        trendFilterService
                                .filterTrend(
                                        stock.getLtp(),
                                        indicators,
                                        regime
                                );

                filtered.setTrendPassed(
                        trend
                );

                filtered.setPassedStage(
                        trend ? 3 : 2
                );

                if (!trend) {

                    filtered.setFailedAt(
                            "TREND"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // STEP 4 - MOMENTUM
                // =================================================

                boolean momentum =
                        momentumFilterService
                                .filterMomentum(
                                        indicators,
                                        regime
                                );

                filtered.setMomentumPassed(
                        momentum
                );

                filtered.setPassedStage(
                        momentum ? 4 : 3
                );

                if (!momentum) {

                    filtered.setFailedAt(
                            "MOMENTUM"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // STEP 5 - VOLUME + VWAP
                // =================================================

                boolean volumeVwap =
                        volumeVwapFilterService
                                .filterVolumeAndVWAP(
                                        stock.getLtp(),
                                        indicators,
                                        regime
                                );

                filtered.setVolumeVwapPassed(
                        volumeVwap
                );

                filtered.setPassedStage(
                        volumeVwap ? 5 : 4
                );

                if (!volumeVwap) {

                    filtered.setFailedAt(
                            "VOLUME_VWAP"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // STEP 6 - ATR + SETUP
                // =================================================

                String setup =
                        atrSetupFilterService
                                .filterAtrAndSetup(
                                        stock.getLtp(),
                                        indicators,
                                        intraday,
                                        regime
                                );

                boolean atrSetup =
                        !"NONE".equals(setup);

                filtered.setAtrSetupPassed(
                        atrSetup
                );

                filtered.setSetup(
                        setup
                );

                filtered.setPassedStage(
                        atrSetup ? 6 : 5
                );

                if (!atrSetup) {

                    filtered.setFailedAt(
                            "ATR_SETUP"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // STEP 7 - ENTRY + RISK
                // =================================================

                Map<String, Double> risk =
                        entryRiskService
                                .filterEntryAndRisk(
                                        stock.getLtp(),
                                        indicators.getAtr(),
                                        setup
                                );

                boolean entryRisk =
                        !risk.isEmpty();

                filtered.setEntryRiskPassed(
                        entryRisk
                );

                filtered.setPassedStage(
                        entryRisk ? 7 : 6
                );

                if (!entryRisk) {

                    filtered.setFailedAt(
                            "ENTRY_RISK"
                    );

                    browserHandler.sendAlgoData(
                            filtered
                    );

                    continue;
                }

                // =================================================
                // FINAL PASS
                // =================================================

                filtered.setSetup(setup);

                filtered.setSignal(
                        "BULLISH_BREAKOUT".equals(setup)
                                ? "BUY"
                                : "BEARISH_BREAKDOWN".equals(setup)
                                ? "SELL"
                                : null
                );

                filtered.setEntry(
                        risk.get("entry")
                );

                filtered.setStopLoss(
                        risk.get("stopLoss")
                );

                filtered.setTarget(
                        risk.get("target")
                );

                filtered.setRisk(
                        risk.get("risk")
                );

                filtered.setReward(
                        risk.get("reward")
                );

                filtered.setRiskReward(
                        risk.get("riskReward")
                );

                filtered.setPassedStage(7);

                filtered.setFailedAt(null);

                // =================================================
                // SEND FINAL ALGO RESULT TO FRONTEND
                // =================================================

                browserHandler.sendAlgoData(
                        filtered
                );

                // =================================================
                // EXISTING ALGOSTOCK
                // =================================================

                AlgoStock result =
                        buildResult(
                                stock,
                                indicators,
                                regime,
                                setup,
                                risk
                        );

                finalStocks.add(result);

            } catch (Exception e) {

                System.err.println(
                        "ALGO SCAN ERROR: "
                                + stock.getSymbol()
                );

                e.printStackTrace();
            }
        }

        return finalStocks
                .stream()
                .sorted(
                        Comparator.comparingDouble(
                                AlgoStock::getRiskReward
                        ).reversed()
                )
                .toList();
    }

    private AlgoStock buildResult(
            MarketTick stock,
            IndicatorResult i,
            String regime,
            String setup,
            Map<String, Double> risk) {

        AlgoStock result =
                new AlgoStock();

        result.setInstrumentKey(
                stock.getInstrumentKey()
        );

        result.setSymbol(
                stock.getSymbol()
        );

        result.setLtp(
                stock.getLtp()
        );

        result.setMarketRegime(
                regime
        );

        result.setLiquidityPassed(true);
        result.setTrendPassed(true);
        result.setMomentumPassed(true);
        result.setVolumeVwapPassed(true);
        result.setAtrSetupPassed(true);
        result.setEntryRiskPassed(true);

        result.setEma20(i.getEma20());
        result.setEma50(i.getEma50());
        result.setEma200(i.getEma200());

        result.setRsi(i.getRsi());

        result.setMacd(i.getMacd());
        result.setMacdSignal(i.getMacdSignal());

        result.setRoc(i.getRoc());

        result.setVwap(i.getVwap());
        result.setAtr(i.getAtr());

        result.setVolumeRatio(
                i.getVolumeRatio()
        );

        result.setSetup(setup);

        if ("BULLISH_BREAKOUT".equals(setup)) {

            result.setSignal("BUY");

        } else if ("BEARISH_BREAKDOWN".equals(setup)) {

            result.setSignal("SELL");
        }

        double bullish = 0;
        double bearish = 0;

        if (stock.getLtp() > i.getEma20()) {
            bullish += 20;
        } else {
            bearish += 20;
        }

        if (i.getEma20() > i.getEma50()) {
            bullish += 15;
        } else {
            bearish += 15;
        }

        if (i.getEma50() > i.getEma200()) {
            bullish += 15;
        } else {
            bearish += 15;
        }

        if (i.getRsi() > 50) {
            bullish += 15;
        } else {
            bearish += 15;
        }

        if (i.getMacd() > i.getMacdSignal()) {
            bullish += 15;
        } else {
            bearish += 15;
        }

        if (stock.getLtp() > i.getVwap()) {
            bullish += 10;
        } else {
            bearish += 10;
        }

        if (i.getRoc() > 0) {
            bullish += 10;
        } else {
            bearish += 10;
        }

        result.setBullishStrength(bullish);
        result.setBearishStrength(bearish);

        result.setEntry(
                risk.get("entry")
        );

        result.setStopLoss(
                risk.get("stopLoss")
        );

        result.setTarget(
                risk.get("target")
        );

        result.setRisk(
                risk.get("risk")
        );

        result.setReward(
                risk.get("reward")
        );

        result.setRiskReward(
                risk.get("riskReward")
        );

        return result;
    }
}