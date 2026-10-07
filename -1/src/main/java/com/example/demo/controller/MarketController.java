package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MarketController {

    // Existing main market page
    @GetMapping("/")
    public String marketPage() {
        return "market";
    }
}