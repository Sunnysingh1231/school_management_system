package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.EmaTrade;
import com.example.demo.service.EmaBuySellFilterService;

@RestController
@RequestMapping("/api/ema")
public class EmaTradeController {
	
	

    private final EmaBuySellFilterService emaBuySellFilterService;

    public EmaTradeController(
            EmaBuySellFilterService emaBuySellFilterService) {
        this.emaBuySellFilterService = emaBuySellFilterService;
    }

    @GetMapping("/trades")
    public List<EmaTrade> getAllTrades() {

        return emaBuySellFilterService.getAllTrades();
    }
}
