package com.example.demo.dto;

public class BuyOrderRequest {

    private String instrumentKey;
    private Integer quantity;

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}