package com.example.demo.model;

import java.time.ZonedDateTime;

public class Candle {

    private ZonedDateTime timestamp;

    private double open;
    private double high;
    private double low;
    private double close;

    private long volume;

    public Candle() {
    }

    public Candle(
            ZonedDateTime timestamp,
            double open,
            double high,
            double low,
            double close,
            long volume) {

        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public ZonedDateTime getTimestamp() {
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
}