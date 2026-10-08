package com.example.demo.controller;

import com.example.demo.model.Candle;
import com.example.demo.service.Nifty50FiveMinuteHistoricalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nifty50")
public class Nifty50ChartController {

    private final Nifty50FiveMinuteHistoricalService historicalService;

    public Nifty50ChartController(
            Nifty50FiveMinuteHistoricalService historicalService) {

        this.historicalService = historicalService;
    }

    // =========================================================
    // NIFTY 50 STOCK LIST
    // =========================================================

    @GetMapping("/stocks")
    public Map<String, String> getStocks() {

        Map<String, String> stocks = new LinkedHashMap<>();

        stocks.put("NSE_EQ|INE090A01021", "ICICI BANK");
        stocks.put("NSE_EQ|INE062A01020", "STATE BANK OF INDIA");
        stocks.put("NSE_EQ|INE002A01018", "RELIANCE");
        stocks.put("NSE_EQ|INE467B01029", "TATA CONSULTANCY SERVICES");
        stocks.put("NSE_EQ|INE009A01021", "INFOSYS");
        stocks.put("NSE_EQ|INE397D01024", "BHARTI AIRTEL");
        stocks.put("NSE_EQ|INE018A01030", "LARSEN & TOUBRO");
        stocks.put("NSE_EQ|INE238A01034", "AXIS BANK");
        stocks.put("NSE_EQ|INE040A01034", "HDFC BANK");

        return stocks;
    }


    // =========================================================
    // 5 MINUTE HISTORICAL CANDLES
    // =========================================================

    @GetMapping("/candles")
    public List<Candle> getCandles(
            @RequestParam String instrumentKey) throws Exception {

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Fetching 5 Minute Historical Data"
        );

        System.out.println(
                "Instrument : " + instrumentKey
        );

        List<Candle> candles =
                historicalService.getPrevious20Days5Minute(
                        instrumentKey
                );

        System.out.println(
                "Candles Returned : " + candles.size()
        );

        System.out.println(
                "=========================================="
        );

        return candles;
    }
}