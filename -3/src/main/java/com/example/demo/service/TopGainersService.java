package com.example.demo.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.model.MarketTick;

@Service
public class TopGainersService {

	private final Map<String, MarketTick> allStocks = new ConcurrentHashMap<>();

	private static final int TOP_COUNT = 10;

	public void update(MarketTick tick) {

		if (tick == null) {
			return;
		}

		String instrumentKey = tick.getInstrumentKey();

		if (instrumentKey == null || !instrumentKey.startsWith("NSE_EQ|")) {
			return;
		}

		allStocks.put(instrumentKey, tick);
	}

	public List<MarketTick> getTopGainers() {
		

		return allStocks.values().stream()

				.filter(tick -> tick.getChangePercent() > 0)

				.sorted(Comparator.comparingDouble(MarketTick::getChangePercent).reversed())

				.limit(TOP_COUNT)

				.collect(Collectors.toList());
	}
}