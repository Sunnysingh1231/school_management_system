package com.example.demo.service;

import com.example.demo.model.MarketTick;
import org.springframework.stereotype.Service;

@Service
public class LiquidityFilterService {

    public boolean filterLiquidity(
            MarketTick stock) {

        if (stock == null ||
                stock.getLtp() <= 0) {

            return false;
        }

        long volume =
                stock.getVolume();

        long buy =
                stock.getTotByQ();

        long sell =
                stock.getTotSlQ();

        double tradedValue =
                stock.getLtp()
                        * volume;

        boolean volumeOk =
                volume >= 100_000;

        boolean valueOk =
                tradedValue >= 10_000_000;

        boolean depthOk =
                buy > 0 || sell > 0;

        return volumeOk &&
                valueOk &&
                depthOk;
    }
}