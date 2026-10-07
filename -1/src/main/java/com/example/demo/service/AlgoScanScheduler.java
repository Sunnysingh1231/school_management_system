package com.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlgoScanScheduler {

    private final AlgoStockFilterService algoStockFilterService;

    public AlgoScanScheduler(
            AlgoStockFilterService algoStockFilterService) {

        this.algoStockFilterService =
                algoStockFilterService;
    }

    /*
     * Har 5 second mein algorithm scan.
     */
    @Scheduled(fixedDelay = 5000)
    public void runAlgoScan() {

        try {

            algoStockFilterService.scanStocks();

        } catch (Exception e) {

            System.err.println(
                    "ALGO SCHEDULER ERROR"
            );

            e.printStackTrace();
        }
    }
}