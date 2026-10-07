package com.example.demo.service;

import com.example.demo.model.MarketTick;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataStore {

    private final Map<String, MarketTick> marketData =
            new ConcurrentHashMap<>();

    public void update(MarketTick tick) {

        if (tick == null) {
            return;
        }

        if (tick.getInstrumentKey() == null) {
            return;
        }

        marketData.put(
                tick.getInstrumentKey(),
                tick
        );

    }
    
    

    public MarketTick getStock(String instrumentKey) {

        return marketData.get(instrumentKey);
    }

    public List<MarketTick> getAllStocks() {

        return new ArrayList<>(marketData.values());
    }

    public List<MarketTick> getRandomStocks(int count) {

        List<MarketTick> stocks =
                new ArrayList<>(marketData.values());

        Collections.shuffle(stocks);

        return stocks.stream()
                .filter(stock ->
                        stock.getSymbol() != null &&
                        
                        stock.getLtp() > 0 && stock.getLtp() > stock.getOpen()
                )
                .limit(count)
                .toList();
    }
}