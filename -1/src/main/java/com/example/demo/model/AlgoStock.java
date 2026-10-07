package com.example.demo.model;

public class AlgoStock {

    private String instrumentKey;
    private String symbol;

    private double ltp;

    private String marketRegime;

    private boolean liquidityPassed;
    private boolean trendPassed;
    private boolean momentumPassed;
    private boolean volumeVwapPassed;
    private boolean atrSetupPassed;
    private boolean entryRiskPassed;

    private double ema20;
    private double ema50;
    private double ema200;

    private double rsi;
    private double macd;
    private double macdSignal;
    private double roc;

    private double vwap;
    private double atr;

    private double volumeRatio;

    private double bullishStrength;
    private double bearishStrength;

    private String setup;
    private String signal;

    private double entry;
    private double stopLoss;
    private double target;
    private double risk;
    private double reward;
    private double riskReward;

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

    public double getLtp() {
        return ltp;
    }

    public void setLtp(double ltp) {
        this.ltp = ltp;
    }

    public String getMarketRegime() {
        return marketRegime;
    }

    public void setMarketRegime(String marketRegime) {
        this.marketRegime = marketRegime;
    }

    public boolean isLiquidityPassed() {
        return liquidityPassed;
    }

    public void setLiquidityPassed(boolean liquidityPassed) {
        this.liquidityPassed = liquidityPassed;
    }

    public boolean isTrendPassed() {
        return trendPassed;
    }

    public void setTrendPassed(boolean trendPassed) {
        this.trendPassed = trendPassed;
    }

    public boolean isMomentumPassed() {
        return momentumPassed;
    }

    public void setMomentumPassed(boolean momentumPassed) {
        this.momentumPassed = momentumPassed;
    }

    public boolean isVolumeVwapPassed() {
        return volumeVwapPassed;
    }

    public void setVolumeVwapPassed(boolean volumeVwapPassed) {
        this.volumeVwapPassed = volumeVwapPassed;
    }

    public boolean isAtrSetupPassed() {
        return atrSetupPassed;
    }

    public void setAtrSetupPassed(boolean atrSetupPassed) {
        this.atrSetupPassed = atrSetupPassed;
    }

    public boolean isEntryRiskPassed() {
        return entryRiskPassed;
    }

    public void setEntryRiskPassed(boolean entryRiskPassed) {
        this.entryRiskPassed = entryRiskPassed;
    }

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

    public double getVolumeRatio() {
        return volumeRatio;
    }

    public void setVolumeRatio(double volumeRatio) {
        this.volumeRatio = volumeRatio;
    }

    public double getBullishStrength() {
        return bullishStrength;
    }

    public void setBullishStrength(double bullishStrength) {
        this.bullishStrength = bullishStrength;
    }

    public double getBearishStrength() {
        return bearishStrength;
    }

    public void setBearishStrength(double bearishStrength) {
        this.bearishStrength = bearishStrength;
    }

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public double getEntry() {
        return entry;
    }

    public void setEntry(double entry) {
        this.entry = entry;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public double getRisk() {
        return risk;
    }

    public void setRisk(double risk) {
        this.risk = risk;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }

    public double getRiskReward() {
        return riskReward;
    }

    public void setRiskReward(double riskReward) {
        this.riskReward = riskReward;
    }
}