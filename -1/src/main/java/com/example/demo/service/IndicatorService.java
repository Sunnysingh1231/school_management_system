package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorService {

    public IndicatorResult calculate(
            List<Candle> daily,
            List<Candle> intraday) {

        IndicatorResult result =
                new IndicatorResult();

        if (daily == null || daily.size() < 200) {
            return result;
        }

        if (intraday == null || intraday.size() < 30) {
            return result;
        }

        List<Double> dailyClose =
                daily.stream()
                        .map(Candle::getClose)
                        .toList();

        List<Double> intradayClose =
                intraday.stream()
                        .map(Candle::getClose)
                        .toList();

        result.setEma20(
                ema(dailyClose, 20)
        );

        result.setEma50(
                ema(dailyClose, 50)
        );

        result.setEma200(
                ema(dailyClose, 200)
        );

        result.setRsi(
                rsi(intradayClose, 14)
        );

        double ema12 =
                ema(intradayClose, 12);

        double ema26 =
                ema(intradayClose, 26);

        double macd =
                ema12 - ema26;

        List<Double> macdValues =
                new ArrayList<>();

        for (int i = 0; i < intradayClose.size(); i++) {

            List<Double> sub =
                    intradayClose.subList(
                            0,
                            i + 1
                    );

            if (sub.size() >= 26) {

                macdValues.add(
                        ema(sub, 12)
                                -
                        ema(sub, 26)
                );
            }
        }

        double macdSignal =
                macdValues.size() >= 9
                        ? ema(macdValues, 9)
                        : 0;

        result.setMacd(macd);
        result.setMacdSignal(macdSignal);
        result.setMacdHistogram(
                macd - macdSignal
        );

        result.setRoc(
                roc(intradayClose, 12)
        );

        result.setVwap(
                calculateVwap(intraday)
        );

        result.setAtr(
                atr(intraday, 14)
        );

        double averageVolume =
                averageVolume(
                        intraday,
                        20
                );

        double latestVolume =
                intraday
                        .get(intraday.size() - 1)
                        .getVolume();

        result.setAverageVolume(
                averageVolume
        );

        result.setVolumeRatio(
                averageVolume > 0
                        ? latestVolume / averageVolume
                        : 0
        );

        return result;
    }

    public double ema(
            List<Double> values,
            int period) {

        if (values == null ||
                values.size() < period) {

            return 0;
        }

        double sum = 0;

        for (int i = 0; i < period; i++) {
            sum += values.get(i);
        }

        double ema =
                sum / period;

        double multiplier =
                2.0 / (period + 1);

        for (int i = period; i < values.size(); i++) {

            ema =
                    (values.get(i) - ema)
                            * multiplier
                            + ema;
        }

        return ema;
    }

    public double rsi(
            List<Double> closes,
            int period) {

        if (closes.size() <= period) {
            return 0;
        }

        double gain = 0;
        double loss = 0;

        for (int i = 1; i <= period; i++) {

            double diff =
                    closes.get(i)
                            -
                    closes.get(i - 1);

            if (diff >= 0) {
                gain += diff;
            } else {
                loss -= diff;
            }
        }

        double avgGain =
                gain / period;

        double avgLoss =
                loss / period;

        for (int i = period + 1;
             i < closes.size();
             i++) {

            double diff =
                    closes.get(i)
                            -
                    closes.get(i - 1);

            double currentGain =
                    Math.max(diff, 0);

            double currentLoss =
                    Math.max(-diff, 0);

            avgGain =
                    ((avgGain * (period - 1))
                            + currentGain)
                            / period;

            avgLoss =
                    ((avgLoss * (period - 1))
                            + currentLoss)
                            / period;
        }

        if (avgLoss == 0) {
            return 100;
        }

        double rs =
                avgGain / avgLoss;

        return 100 - (100 / (1 + rs));
    }

    public double roc(
            List<Double> closes,
            int period) {

        if (closes.size() <= period) {
            return 0;
        }

        double current =
                closes.get(
                        closes.size() - 1
                );

        double old =
                closes.get(
                        closes.size() - 1 - period
                );

        if (old == 0) {
            return 0;
        }

        return ((current - old) / old) * 100;
    }

    public double calculateVwap(
            List<Candle> candles) {

        double cumulativePV = 0;
        double cumulativeVolume = 0;

        for (Candle candle : candles) {

            double typicalPrice =
                    (
                            candle.getHigh()
                                    +
                            candle.getLow()
                                    +
                            candle.getClose()
                    ) / 3.0;

            cumulativePV +=
                    typicalPrice
                            * candle.getVolume();

            cumulativeVolume +=
                    candle.getVolume();
        }

        if (cumulativeVolume == 0) {
            return 0;
        }

        return cumulativePV /
                cumulativeVolume;
    }

    public double atr(
            List<Candle> candles,
            int period) {

        if (candles.size() < period + 1) {
            return 0;
        }

        List<Double> trueRanges =
                new ArrayList<>();

        for (int i = 1;
             i < candles.size();
             i++) {

            Candle current =
                    candles.get(i);

            Candle previous =
                    candles.get(i - 1);

            double tr1 =
                    current.getHigh()
                            -
                    current.getLow();

            double tr2 =
                    Math.abs(
                            current.getHigh()
                                    -
                            previous.getClose()
                    );

            double tr3 =
                    Math.abs(
                            current.getLow()
                                    -
                            previous.getClose()
                    );

            trueRanges.add(
                    Math.max(
                            tr1,
                            Math.max(tr2, tr3)
                    )
            );
        }

        if (trueRanges.size() < period) {
            return 0;
        }

        double sum = 0;

        for (int i =
                     trueRanges.size() - period;
             i < trueRanges.size();
             i++) {

            sum += trueRanges.get(i);
        }

        return sum / period;
    }

    private double averageVolume(
            List<Candle> candles,
            int period) {

        if (candles.size() < period) {
            return 0;
        }

        long total = 0;

        for (int i =
                     candles.size() - period;
             i < candles.size();
             i++) {

            total +=
                    candles.get(i).getVolume();
        }

        return (double) total / period;
    }
}