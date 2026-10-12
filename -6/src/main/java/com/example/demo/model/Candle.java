package com.example.demo.model;

import java.time.LocalDateTime;

public class Candle {

	private String instrumentKey;

	// Exact candle date + time
	private LocalDateTime timestamp;

	private double open;
	private double high;
	private double low;
	private double close;

	private long volume;

	// =========================================================
	// CONSTRUCTOR
	// =========================================================

	public Candle() {
	}

	public Candle(String instrumentKey, LocalDateTime timestamp, double open, double high, double low, double close,
			long volume) {

		this.instrumentKey = instrumentKey;
		this.timestamp = timestamp;

		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;

		this.volume = volume;
	}

	// =========================================================
	// GETTERS
	// =========================================================

	public String getInstrumentKey() {
		return instrumentKey;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public double getOpen() {
		return open;
	}

	public double getHigh() {
		return high;
	}

	public double getLow() {
		return low;
	}

	public double getClose() {
		return close;
	}

	public long getVolume() {
		return volume;
	}

	// =========================================================
	// SETTERS
	// =========================================================

	public void setInstrumentKey(String instrumentKey) {
		this.instrumentKey = instrumentKey;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public void setVolume(long volume) {
		this.volume = volume;
	}

	// =========================================================
	// TO STRING
	// =========================================================

	@Override
	public String toString() {

		return timestamp + " | O: " + open + " | H: " + high + " | L: " + low + " | C: " + close + " | V: " + volume;
	}
}