package com.example.demo.controller;

import com.example.demo.model.Candle;
import com.example.demo.service.Nifty50FiveMinuteHistoricalService;
import com.example.demo.service.TrendFilterService;
import com.example.demo.service.UpstoxMarketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nifty50")
public class Nifty50ChartController {

    private final UpstoxMarketService upstoxMarketService;

    private final Nifty50FiveMinuteHistoricalService historicalService;

    private final TrendFilterService trendFilterService;

    public Nifty50ChartController(
            UpstoxMarketService upstoxMarketService,
            Nifty50FiveMinuteHistoricalService historicalService,
            TrendFilterService trendFilterService) {

        this.upstoxMarketService = upstoxMarketService;

        this.historicalService = historicalService;

        this.trendFilterService = trendFilterService;
    }

    // =========================================================
    // NIFTY 50 STOCK LIST
    // =========================================================

    @GetMapping("/stocks")
    public Map<String, String> getStocks() {

        return upstoxMarketService.getNifty50Stocks();
    }

    // =========================================================
    // HISTORICAL CANDLE DATA
    // =========================================================

    @GetMapping("/candles")
    public List<Candle> getCandles(
            @RequestParam String instrumentKey)
            throws Exception {

        return historicalService.getPrevious20Days5Minute(
                instrumentKey
        );
    }

    // =========================================================
    // EMA 20 / 50 / 200 + TREND
    // =========================================================

    @GetMapping("/trend")
    public TrendFilterService.TrendResult getTrend(
            @RequestParam String instrumentKey)
            throws Exception {

        return trendFilterService.analyzeTrend(
                instrumentKey
        );
    }
}