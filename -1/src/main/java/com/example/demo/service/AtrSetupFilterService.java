package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtrSetupFilterService {

    public String filterAtrAndSetup(
            double price,
            IndicatorResult i,
            List<Candle> candles,
            String regime) {

        if (i == null ||
                i.getAtr() <= 0 ||
                candles == null ||
                candles.size() < 2) {

            return "NONE";
        }

        Candle previous =
                candles.get(
                        candles.size() - 2
                );

        Candle current =
                candles.get(
                        candles.size() - 1
                );

        double atr =
                i.getAtr();

        if (atr <= 0) {
            return "NONE";
        }

        if ("BULLISH".equals(regime)) {

            boolean breakout =
                    current.getClose()
                            >
                    previous.getHigh();

            boolean momentumCandle =
                    current.getClose()
                            >
                    current.getOpen();

            boolean rangeOk =
                    (current.getHigh()
                            -
                            current.getLow())
                            >=
                    atr * 0.5;

            if (breakout &&
                    momentumCandle &&
                    rangeOk) {

                return "BULLISH_BREAKOUT";
            }
        }

        if ("BEARISH".equals(regime)) {

            boolean breakdown =
                    current.getClose()
                            <
                    previous.getLow();

            boolean momentumCandle =
                    current.getClose()
                            <
                    current.getOpen();

            boolean rangeOk =
                    (current.getHigh()
                            -
                            current.getLow())
                            >=
                    atr * 0.5;

            if (breakdown &&
                    momentumCandle &&
                    rangeOk) {

                return "BEARISH_BREAKDOWN";
            }
        }

        return "NONE";
    }
}