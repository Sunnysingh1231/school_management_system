package com.example.demo.service;

import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

@Service
public class TrendFilterService {

    public boolean filterTrend(
            double price,
            IndicatorResult i,
            String regime) {

        if (i == null) {
            return false;
        }

        if ("BULLISH".equals(regime)) {

            return price > i.getEma20()
                    &&
                    i.getEma20() > i.getEma50()
                    &&
                    i.getEma50() > i.getEma200();
        }

        if ("BEARISH".equals(regime)) {

            return price < i.getEma20()
                    &&
                    i.getEma20() < i.getEma50()
                    &&
                    i.getEma50() < i.getEma200();
        }

        return false;
    }
}