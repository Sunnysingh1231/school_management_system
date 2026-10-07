package com.example.demo.service;

import com.example.demo.model.IndicatorResult;
import org.springframework.stereotype.Service;

@Service
public class VolumeVwapFilterService {

    public boolean filterVolumeAndVWAP(
            double price,
            IndicatorResult i,
            String regime) {

        if (i == null ||
                i.getVwap() <= 0) {

            return false;
        }

        boolean volumeOk =
                i.getVolumeRatio() >= 1.20;

        if ("BULLISH".equals(regime)) {

            return price > i.getVwap()
                    &&
                    volumeOk;
        }

        if ("BEARISH".equals(regime)) {

            return price < i.getVwap()
                    &&
                    volumeOk;
        }

        return false;
    }
}