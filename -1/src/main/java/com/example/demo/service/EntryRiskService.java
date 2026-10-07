package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EntryRiskService {

    private static final double MIN_RR = 2.0;

    public Map<String, Double> filterEntryAndRisk(
            double entry,
            double atr,
            String setup) {

        Map<String, Double> result =
                new HashMap<>();

        if (entry <= 0 ||
                atr <= 0 ||
                setup == null ||
                "NONE".equals(setup)) {

            return result;
        }

        double stopLoss;
        double target;

        if ("BULLISH_BREAKOUT".equals(setup)) {

            stopLoss =
                    entry - (atr * 1.0);

            double risk =
                    entry - stopLoss;

            target =
                    entry + (risk * MIN_RR);
        }

        else if ("BEARISH_BREAKDOWN".equals(setup)) {

            stopLoss =
                    entry + (atr * 1.0);

            double risk =
                    stopLoss - entry;

            target =
                    entry - (risk * MIN_RR);
        }

        else {
            return result;
        }

        double risk =
                Math.abs(
                        entry - stopLoss
                );

        double reward =
                Math.abs(
                        target - entry
                );

        double rr =
                risk > 0
                        ? reward / risk
                        : 0;

        if (rr < MIN_RR) {
            return result;
        }

        result.put("entry", entry);
        result.put("stopLoss", stopLoss);
        result.put("target", target);
        result.put("risk", risk);
        result.put("reward", reward);
        result.put("riskReward", rr);

        return result;
    }
}