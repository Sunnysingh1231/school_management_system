package com.example.demo.model;

public class IndicatorResult {

    private double ema20;
    private double ema50;
    private double ema200;

    private double rsi;

    private double macd;
    private double macdSignal;
    private double macdHistogram;

    private double roc;

    private double vwap;

    private double atr;

    private double averageVolume;
    private double volumeRatio;

    public double getEma20() {
        return ema20;
    }

    public void setEma20(double ema20) {
        this.ema20 = ema20;
    }

    public double getEma50() {
        return ema50;
    }

    public void setEma50(double ema50) {
        this.ema50 = ema50;
    }

    public double getEma200() {
        return ema200;
    }

    public void setEma200(double ema200) {
        this.ema200 = ema200;
    }

    public double getRsi() {
        return rsi;
    }

    public void setRsi(double rsi) {
        this.rsi = rsi;
    }

    public double getMacd() {
        return macd;
    }

    public void setMacd(double macd) {
        this.macd = macd;
    }

    public double getMacdSignal() {
        return macdSignal;
    }

    public void setMacdSignal(double macdSignal) {
        this.macdSignal = macdSignal;
    }

    public double getMacdHistogram() {
        return macdHistogram;
    }

    public void setMacdHistogram(double macdHistogram) {
        this.macdHistogram = macdHistogram;
    }

    public double getRoc() {
        return roc;
    }

    public void setRoc(double roc) {
        this.roc = roc;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public double getAtr() {
        return atr;
    }

    public void setAtr(double atr) {
        this.atr = atr;
    }

    public double getAverageVolume() {
        return averageVolume;
    }

    public void setAverageVolume(double averageVolume) {
        this.averageVolume = averageVolume;
    }

    public double getVolumeRatio() {
        return volumeRatio;
    }

    public void setVolumeRatio(double volumeRatio) {
        this.volumeRatio = volumeRatio;
    }
}