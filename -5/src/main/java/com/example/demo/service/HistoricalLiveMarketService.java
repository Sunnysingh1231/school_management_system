package com.example.demo.service;

import com.example.demo.model.Candle;
import com.example.demo.webSocket.EmaWebSocketHandler;
import com.example.demo.webSocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoricalLiveMarketService {

	private final Nifty50FiveMinuteHistoricalService historicalService;
	private final WebSocketHandler webSocketHandler;
	private final ObjectMapper objectMapper;
	
	private final EmaWebSocketHandler emaWebSocketHandler;
	
	private final LiveEmaCalculatorService emaCalculatorService;

	public HistoricalLiveMarketService(Nifty50FiveMinuteHistoricalService historicalService,
			WebSocketHandler webSocketHandler, LiveEmaCalculatorService emaCalculatorService, EmaWebSocketHandler emaWebSocketHandler) {

		this.historicalService = historicalService;
		this.webSocketHandler = webSocketHandler;

		objectMapper = new ObjectMapper();
		this.emaWebSocketHandler = emaWebSocketHandler;
		this.emaCalculatorService = emaCalculatorService;

		objectMapper.registerModule(new JavaTimeModule());
	}

	@PostConstruct
	public void start() {

		new Thread(() -> {

			try {

				List<String> instrumentKeys = List.of(

						"NSE_EQ|INE040A01034",
					    "NSE_EQ|INE090A01021",
					    "NSE_EQ|INE062A01020",
					    "NSE_EQ|INE002A01018",
					    "NSE_EQ|INE467B01029",
					    "NSE_EQ|INE009A01021",
					    "NSE_EQ|INE397D01024",
					    "NSE_EQ|INE018A01030",
					    "NSE_EQ|INE238A01034",
					    "NSE_EQ|INE040A01034"

				);

				Map<String, List<Candle>> allStocks = new HashMap<>();

				for (String instrumentKey : instrumentKeys) {

					System.out.println("Loading : " + instrumentKey);

					List<Candle> candles = historicalService.getPrevious20Days5Minute(instrumentKey);

					allStocks.put(instrumentKey, candles);

					System.out.println(instrumentKey + " -> " + candles.size() + " candles");
				}

				int maxCandles = 0;

				for (List<Candle> candles : allStocks.values()) {

					if (candles.size() > maxCandles) {

						maxCandles = candles.size();
					}
				}

				System.out.println("Total Stocks = " + instrumentKeys.size());

				System.out.println("Max Candles = " + maxCandles);

				for (int i = 0; i < maxCandles; i++) {

					for (String instrumentKey : instrumentKeys) {

						List<Candle> candles = allStocks.get(instrumentKey);

						if (i < candles.size()) {

							Candle candle = candles.get(i);
							
							emaCalculatorService.addCandle(candle);


							String json = objectMapper.writeValueAsString(candle);
							
							webSocketHandler.sendMessage(json);
//							emaWebSocketHandler.sendMessage(json);

						}
						
					}
					System.out.println("Total candle "+i+"/"+maxCandles);

					Thread.sleep(500);
				}

			} catch (Exception e) {

				e.printStackTrace();
			}

		}).start();
	}
}