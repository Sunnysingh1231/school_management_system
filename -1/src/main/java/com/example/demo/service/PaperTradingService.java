package com.example.demo.service;

import com.example.demo.dto.BuyOrderRequest;
import com.example.demo.model.MarketTick;
import com.example.demo.model.PaperPosition;
import com.example.demo.model.PaperTradeHistory;
import com.example.demo.repository.PaperPositionRepository;
import com.example.demo.repository.PaperTradeHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaperTradingService {

    private final MarketDataStore marketDataStore;
    private final PaperPositionRepository positionRepository;
    private final PaperTradeHistoryRepository historyRepository;

    public PaperTradingService(
            MarketDataStore marketDataStore,
            PaperPositionRepository positionRepository,
            PaperTradeHistoryRepository historyRepository) {

        this.marketDataStore = marketDataStore;
        this.positionRepository = positionRepository;
        this.historyRepository = historyRepository;
    }

    // ==============================
    // RANDOM 10 STOCKS
    // ==============================

    public List<MarketTick> getRandomStocks() {
        return marketDataStore.getRandomStocks(10);
    }

    // ==============================
    // CURRENT POSITIONS
    // ==============================

    public List<PaperPosition> getPositions() {
        return positionRepository.findAllByOrderByBuyDateDesc();
    }

    // ==============================
    // BUY
    // ==============================

    @Transactional
    public PaperPosition buyStock(BuyOrderRequest request) {

        if (request == null) {
            throw new RuntimeException("Invalid order request");
        }

        if (request.getInstrumentKey() == null ||
                request.getInstrumentKey().isBlank()) {

            throw new RuntimeException("Instrument key is required");
        }

        if (request.getQuantity() == null ||
                request.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }

        MarketTick stock =
                marketDataStore.getStock(request.getInstrumentKey());

        if (stock == null) {
            throw new RuntimeException(
                    "Live stock data not available"
            );
        }

        if (stock.getLtp() <= 0) {
            throw new RuntimeException(
                    "Invalid live price"
            );
        }

        double buyPrice = stock.getLtp();

        // Check existing position
        PaperPosition position =
                positionRepository
                        .findByInstrumentKey(
                                request.getInstrumentKey()
                        )
                        .orElse(null);

        if (position != null &&
                "BOUGHT".equals(position.getStatus())) {

            throw new RuntimeException(
                    "Stock is already bought"
            );
        }

        // Existing SOLD record ko reuse kar sakte hain
        if (position == null) {
            position = new PaperPosition();
        }

        position.setInstrumentKey(stock.getInstrumentKey());
        position.setSymbol(stock.getSymbol());
        position.setStatus("BOUGHT");
        position.setBuyPrice(buyPrice);
        position.setSellPrice(null);
        position.setQuantity(request.getQuantity());
        position.setBuyDate(LocalDateTime.now());
        position.setSellDate(null);

        return positionRepository.save(position);
    }

    // ==============================
    // SELL
    // ==============================

    @Transactional
    public PaperTradeHistory sellStock(
            String instrumentKey) {

        if (instrumentKey == null ||
                instrumentKey.isBlank()) {

            throw new RuntimeException(
                    "Instrument key is required"
            );
        }

        PaperPosition position =
                positionRepository
                        .findByInstrumentKey(instrumentKey)
                        .orElse(null);

        if (position == null ||
                !"BOUGHT".equals(position.getStatus())) {

            throw new RuntimeException(
                    "This stock is not currently bought"
            );
        }

        MarketTick stock =
                marketDataStore.getStock(instrumentKey);

        if (stock == null) {
            throw new RuntimeException(
                    "Live stock data not available"
            );
        }

        if (stock.getLtp() <= 0) {
            throw new RuntimeException(
                    "Invalid live price"
            );
        }

        double sellPrice = stock.getLtp();
        double buyPrice = position.getBuyPrice();

        int quantity = position.getQuantity();

        // P/L per share
        double difference =
                sellPrice - buyPrice;

        // Total P/L
        double profitLoss =
                difference * quantity;

        double profitLossPercent =
                buyPrice != 0
                        ? ((sellPrice - buyPrice)
                        / buyPrice) * 100
                        : 0;

        // ==============================
        // HISTORY
        // ==============================

        PaperTradeHistory history =
                new PaperTradeHistory();

        history.setInstrumentKey(
                position.getInstrumentKey()
        );

        history.setSymbol(
                position.getSymbol()
        );

        history.setBuyPrice(buyPrice);
        history.setSellPrice(sellPrice);
        history.setQuantity(quantity);

        history.setProfitLoss(profitLoss);
        history.setProfitLossPercent(
                profitLossPercent
        );

        history.setBuyDate(
                position.getBuyDate()
        );

        history.setSellDate(
                LocalDateTime.now()
        );

        PaperTradeHistory savedHistory =
                historyRepository.save(history);

        // ==============================
        // CURRENT POSITION = SOLD
        // ==============================

        position.setStatus("SOLD");
        position.setSellPrice(sellPrice);
        position.setSellDate(
                LocalDateTime.now()
        );

        positionRepository.save(position);

        return savedHistory;
    }

    // ==============================
    // HISTORY
    // ==============================

    public List<PaperTradeHistory> getTradeHistory() {

        return historyRepository
                .findAllByOrderBySellDateDesc();
    }

    // ==============================
    // DELETE HISTORY
    // ==============================

    @Transactional
    public void deleteHistory(Long id) {

        if (!historyRepository.existsById(id)) {

            throw new RuntimeException(
                    "Trade history not found"
            );
        }

        historyRepository.deleteById(id);
    }
}