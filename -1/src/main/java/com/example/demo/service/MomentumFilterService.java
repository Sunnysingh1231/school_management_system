package com.example.demo.service;

import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

@Service
public class MomentumFilterService {

    public boolean filterMomentum(
            IndicatorResult i,
            String regime) {

        if (i == null) {
            return false;
        }

        if ("BULLISH".equals(regime)) {

            return i.getRsi() >= 50
                    &&
                    i.getRsi() <= 75
                    &&
                    i.getMacd() > i.getMacdSignal()
                    &&
                    i.getRoc() > 0;
        }

        if ("BEARISH".equals(regime)) {

            return i.getRsi() <= 50
                    &&
                    i.getRsi() >= 25
                    &&
                    i.getMacd() < i.getMacdSignal()
                    &&
                    i.getRoc() < 0;
        }

        return false;
    }
}