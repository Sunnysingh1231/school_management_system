package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "paper_trade_history")
public class PaperTradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instrumentKey;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private Double buyPrice;

    @Column(nullable = false)
    private Double sellPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double profitLoss;

    @Column(nullable = false)
    private Double profitLossPercent;

    @Column(nullable = false)
    private LocalDateTime buyDate;

    @Column(nullable = false)
    private LocalDateTime sellDate;

    public PaperTradeHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public void setInstrumentKey(String instrumentKey) {
        this.instrumentKey = instrumentKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(Double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public Double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(Double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(Double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public Double getProfitLossPercent() {
        return profitLossPercent;
    }

    public void setProfitLossPercent(Double profitLossPercent) {
        this.profitLossPercent = profitLossPercent;
    }

    public LocalDateTime getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDateTime buyDate) {
        this.buyDate = buyDate;
    }

    public LocalDateTime getSellDate() {
        return sellDate;
    }

    public void setSellDate(LocalDateTime sellDate) {
        this.sellDate = sellDate;
    }
}