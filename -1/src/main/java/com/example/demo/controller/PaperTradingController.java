package com.example.demo.controller;

import com.example.demo.dto.BuyOrderRequest;
import com.example.demo.model.MarketTick;
import com.example.demo.model.PaperPosition;
import com.example.demo.model.PaperTradeHistory;
import com.example.demo.service.PaperTradingService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/paper-trading")
public class PaperTradingController {

    private final PaperTradingService paperTradingService;

    public PaperTradingController(
            PaperTradingService paperTradingService) {

        this.paperTradingService =
                paperTradingService;
    }

    // ==============================
    // PAGE
    // ==============================

    @GetMapping
    public String page() {
        return "paper-trading";
    }

    // ==============================
    // RANDOM 10 STOCKS
    // ==============================

    @GetMapping("/stocks")
    @ResponseBody
    public List<MarketTick> getRandomStocks() {

        return paperTradingService
                .getRandomStocks();
    }

    // ==============================
    // CURRENT POSITIONS
    // ==============================

    @GetMapping("/positions")
    @ResponseBody
    public List<PaperPosition> getPositions() {

        return paperTradingService
                .getPositions();
    }

    // ==============================
    // BUY
    // ==============================

    @PostMapping("/buy")
    @ResponseBody
    public ResponseEntity<?> buyStock(
            @RequestBody BuyOrderRequest request) {

        try {

            PaperPosition position =
                    paperTradingService
                            .buyStock(request);

            return ResponseEntity.ok(position);

        } catch (Exception e) {

            Map<String, String> response =
                    new HashMap<>();

            response.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }
    }

    // ==============================
    // SELL
    // ==============================

    @PostMapping("/sell")
    @ResponseBody
    public ResponseEntity<?> sellStock(
            @RequestParam String instrumentKey) {

        try {

            PaperTradeHistory history =
                    paperTradingService
                            .sellStock(
                                    instrumentKey
                            );

            return ResponseEntity.ok(history);

        } catch (Exception e) {

            Map<String, String> response =
                    new HashMap<>();

            response.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }
    }

    // ==============================
    // TRADE HISTORY
    // ==============================

    @GetMapping("/history")
    @ResponseBody
    public List<PaperTradeHistory>
    getTradeHistory() {

        return paperTradingService
                .getTradeHistory();
    }

    // ==============================
    // DELETE HISTORY
    // ==============================

    @DeleteMapping("/history/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteHistory(
            @PathVariable Long id) {

        try {

            paperTradingService
                    .deleteHistory(id);

            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }
}