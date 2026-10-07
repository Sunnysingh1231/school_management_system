package com.example.demo.controller;

import com.example.demo.model.AlgoStock;
import com.example.demo.service.AlgoStockFilterService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/algo-tradingg")
public class AlgoTradingController {

    private final AlgoStockFilterService algoStockFilterService;

    public AlgoTradingController(
            AlgoStockFilterService algoStockFilterService) {

        this.algoStockFilterService =
                algoStockFilterService;
    }

    @GetMapping
    public String page() {
        return "algo-trading";
    }

    @GetMapping("/stocks")
    @ResponseBody
    public ResponseEntity<?> getStocks() {

        try {

            List<AlgoStock> stocks =
                    algoStockFilterService.scanStocks();

            return ResponseEntity.ok(stocks);

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