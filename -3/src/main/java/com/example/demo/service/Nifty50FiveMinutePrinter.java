package com.example.demo.service;

import com.example.demo.model.Candle;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class Nifty50FiveMinutePrinter {

	private final UpstoxMarketService upstoxMarketService;

	private final Nifty50FiveMinuteHistoricalService historicalService;

	public Nifty50FiveMinutePrinter(UpstoxMarketService upstoxMarketService,
			Nifty50FiveMinuteHistoricalService historicalService) {

		this.upstoxMarketService = upstoxMarketService;
		this.historicalService = historicalService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void printData() {

//        System.out.println();
//        System.out.println("================================================");
//        System.out.println("NIFTY 50 - 5 MINUTE HISTORICAL DATA");
//        System.out.println("TIME : 09:15 AM TO 10:00 AM");
//        System.out.println("PREVIOUS 20 TRADING DAYS");
//        System.out.println("================================================");

		Map<String, String> stocks = upstoxMarketService.getNifty50Stocks();

//        System.out.println(
//                "Stocks found: " + stocks.size()
//        );

		LocalTime startTime = LocalTime.of(9, 15);
		LocalTime endTime = LocalTime.of(10, 0);

		for (Map.Entry<String, String> entry : stocks.entrySet()) {

			String instrumentKey = entry.getKey();
			String symbol = entry.getValue();

			try {

//                System.out.println();
//                System.out.println(
//                        "\n################################################"
//                );
//
//                System.out.println(
//                        "STOCK : " + symbol
//                );
//
//                System.out.println(
//                        "KEY   : " + instrumentKey
//                );
//
//                System.out.println(
//                        "TIME  : 09:15 - 10:00"
//                );
//
//                System.out.println(
//                        "################################################"
//                );

				List<Candle> candles = historicalService.getPrevious20Days5Minute(instrumentKey);

				int printedCount = 0;

				for (Candle candle : candles) {

					LocalTime candleTime = candle.getTimestamp().toLocalTime();

					/*
					 * Sirf 09:15 se 10:00 tak
					 */
					if (!candleTime.isBefore(startTime) && candleTime.isBefore(endTime)) {

//                        System.out.println(
//                                symbol
//                                        + " | "
//                                        + candle
//                        );

						printedCount++;
					}
				}

//                System.out.println();
//                System.out.println(
//                        "Printed 5M candles : "
//                                + printedCount
//                );
//
//                System.out.println(
//                        "Finished : " + symbol
//                );

			} catch (Exception e) {

				System.err.println("ERROR : " + symbol + " | " + e.getMessage());
			}
		}

//        System.out.println();
//        System.out.println(
//                "================================================"
//        );
//
//        System.out.println(
//                "NIFTY 50 09:15-10:00 DATA COMPLETED"
//        );
//
//        System.out.println(
//                "================================================"
//        );
	}
}