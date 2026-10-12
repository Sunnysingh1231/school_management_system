package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ema_trades")
public class EmaTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instrumentKey;

    private Double buyPrice;

    private Double sellPrice;

    private Double changePercentage;

    private Double profitLoss;

    private Double bullishStrength;

    private Double bearishStrength;

    private Double ema20;

    private Double ema50;

    private Double ema200;

    private String trend;

    private String status;

    private LocalDateTime buyTime;

    private LocalDateTime sellTime;


    public EmaTrade() {
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

    public Double getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(Double changePercentage) {
        this.changePercentage = changePercentage;
    }

    public Double getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(Double profitLoss) {
        this.profitLoss = profitLoss;
    }

    public Double getBullishStrength() {
        return bullishStrength;
    }

    public void setBullishStrength(Double bullishStrength) {
        this.bullishStrength = bullishStrength;
    }

    public Double getBearishStrength() {
        return bearishStrength;
    }

    public void setBearishStrength(Double bearishStrength) {
        this.bearishStrength = bearishStrength;
    }

    public Double getEma20() {
        return ema20;
    }

    public void setEma20(Double ema20) {
        this.ema20 = ema20;
    }

    public Double getEma50() {
        return ema50;
    }

    public void setEma50(Double ema50) {
        this.ema50 = ema50;
    }

    public Double getEma200() {
        return ema200;
    }

    public void setEma200(Double ema200) {
        this.ema200 = ema200;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(LocalDateTime buyTime) {
        this.buyTime = buyTime;
    }

    public LocalDateTime getSellTime() {
        return sellTime;
    }

    public void setSellTime(LocalDateTime sellTime) {
        this.sellTime = sellTime;
    }
}