package com.example.demo.model;

import java.util.List;

public class MarketTick {

    private String instrumentKey;

    private String symbol;

    private double ltp;

    private double change;

    private double changePercent;

    private double open;

    private double high;

    private double low;

    private double previousClose;

    private long volume;

    private long lastQuantity;
    
    private long totByQ;

    private long totSlQ;

    private String updateTime;
    
    private List<DepthLevel> bidLevels;
    private List<DepthLevel> askLevels;

    public MarketTick() {
    }

    

    public MarketTick(String instrumentKey, String symbol, double ltp, double change, double changePercent, double open,
			double high, double low, double previousClose, long volume, long lastQuantity, long totByQ, long totSlQ,
			String updateTime, List<DepthLevel> bidLevels, List<DepthLevel> askLevels) {
		super();
		this.instrumentKey = instrumentKey;
		this.symbol = symbol;
		this.ltp = ltp;
		this.change = change;
		this.changePercent = changePercent;
		this.open = open;
		this.high = high;
		this.low = low;
		this.previousClose = previousClose;
		this.volume = volume;
		this.lastQuantity = lastQuantity;
		this.totByQ = totByQ;
		this.totSlQ = totSlQ;
		this.updateTime = updateTime;
		this.bidLevels = bidLevels;
		this.askLevels = askLevels;
	}

	public long getTotByQ() {
		return totByQ;
	}



	public void setTotByQ(long totByQ) {
		this.totByQ = totByQ;
	}



	public long getTotSlQ() {
		return totSlQ;
	}



	public void setTotSlQ(long totSlQ) {
		this.totSlQ = totSlQ;
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

    public double getLtp() {
        return ltp;
    }

    public void setLtp(double ltp) {
        this.ltp = ltp;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getLastQuantity() {
        return lastQuantity;
    }

    public void setLastQuantity(long lastQuantity) {
        this.lastQuantity = lastQuantity;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    public List<DepthLevel> getBidLevels() {
        return bidLevels;
    }

    public void setBidLevels(List<DepthLevel> bidLevels) {
        this.bidLevels = bidLevels;
    }


    public List<DepthLevel> getAskLevels() {
        return askLevels;
    }

    public void setAskLevels(List<DepthLevel> askLevels) {
        this.askLevels = askLevels;
    }
}