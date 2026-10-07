package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarketController {

    private final LivePriceService livePriceService;


    public MarketController(
            LivePriceService livePriceService) {

        this.livePriceService =
                livePriceService;
    }


    @GetMapping("/market")
    public String market(Model model) {

        model.addAttribute(
                "prices",
                livePriceService.getPrices()
        );

        return "market";
    }
}