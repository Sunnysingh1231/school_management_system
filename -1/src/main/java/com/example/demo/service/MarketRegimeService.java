package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketRegimeService {

    private final IndicatorService indicatorService;

    public MarketRegimeService(
            IndicatorService indicatorService) {

        this.indicatorService =
                indicatorService;
    }

    public String filterMarketRegime(
            List<Candle> niftyDaily) {

        if (niftyDaily == null ||
                niftyDaily.size() < 200) {

            return "NO_DATA";
        }

        List<Double> closes =
                niftyDaily.stream()
                        .map(Candle::getClose)
                        .toList();

        double price =
                closes.get(
                        closes.size() - 1
                );

        double ema20 =
                indicatorService.ema(
                        closes,
                        20
                );

        double ema50 =
                indicatorService.ema(
                        closes,
                        50
                );

        double ema200 =
                indicatorService.ema(
                        closes,
                        200
                );

        if (price > ema20 &&
                ema20 > ema50 &&
                ema50 > ema200) {

            return "BULLISH";
        }

        if (price < ema20 &&
                ema20 < ema50 &&
                ema50 < ema200) {

            return "BEARISH";
        }

        return "SIDEWAYS";
    }
}