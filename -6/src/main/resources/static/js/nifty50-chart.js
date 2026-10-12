let chart = null;

let candleSeries = null;

let ema20Series = null;
let ema50Series = null;
let ema200Series = null;

/*
 * Kitne trading days chart par dikhane hain.
 * EMA poori history par calculate hota hai, sirf visible range 20 din ki hai.
 * Left scroll karne par purana data bhi dikhega.
 */
const DISPLAY_TRADING_DAYS = 20;


/* =====================================================
   INITIALIZE CHART
===================================================== */

function initializeChart() {

    const container = document.getElementById("chart");

    chart = LightweightCharts.createChart(container, {
        width: container.clientWidth,
        height: 650,

        layout: {
            background: { color: "#111827" },
            textColor: "#9ca3af"
        },

        grid: {
            vertLines: { color: "#1f2937" },
            horzLines: { color: "#1f2937" }
        },

        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal
        },

        rightPriceScale: {
            borderColor: "#374151"
        },

        timeScale: {
            borderColor: "#374151",
            timeVisible: true,
            secondsVisible: false
        }
    });


    candleSeries = chart.addSeries(LightweightCharts.CandlestickSeries, {
        upColor: "#22c55e",
        downColor: "#ef4444",
        borderVisible: false,
        wickUpColor: "#22c55e",
        wickDownColor: "#ef4444"
    });

    ema20Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#3b82f6",
        lineWidth: 2,
        title: "EMA 20",
        priceLineVisible: false,
        lastValueVisible: true
    });

    ema50Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#f59e0b",
        lineWidth: 2,
        title: "EMA 50",
        priceLineVisible: false,
        lastValueVisible: true
    });

    ema200Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#a855f7",
        lineWidth: 2,
        title: "EMA 200",
        priceLineVisible: false,
        lastValueVisible: true
    });


    window.addEventListener("resize", () => {
        chart.applyOptions({ width: container.clientWidth });
    });
}


/* =====================================================
   LOAD STOCK LIST
===================================================== */

async function loadStocks() {

    try {

        const response = await fetch("/api/nifty50/stocks");

        if (!response.ok) {
            throw new Error("Stock API failed: " + response.status);
        }

        const stocks = await response.json();

        const select = document.getElementById("stockSelect");

        select.innerHTML = '<option value="">Select Stock</option>';

        Object.entries(stocks).forEach(([instrumentKey, symbol]) => {

            const option = document.createElement("option");

            option.value = instrumentKey;
            option.textContent = symbol;

            select.appendChild(option);
        });

        console.log("Stocks loaded:", Object.keys(stocks).length);

    } catch (error) {

        console.error("Stock loading error:", error);
    }
}


/* =====================================================
   TIME HELPER (TIMEZONE FIX)
===================================================== */

/*
 * Backend ka timestamp exchange (IST) local time hai, e.g. 2026-09-30T09:15:00
 *
 * Lightweight Charts time ko UTC maan kar dikhata hai. Agar hum new Date(...)
 * use karein to browser timezone apply hota hai aur axis par time shift ho jata hai.
 *
 * Isliye components ko seedha Date.UTC me daal rahe hain:
 * 09:15 candle hamesha 09:15 dikhega, chahe browser kisi bhi timezone me ho.
 */
function toChartTime(ts) {

    let y, mo, d, h, mi, s = 0;

    if (Array.isArray(ts)) {

        // Jackson kabhi kabhi [2026, 9, 30, 9, 15] array bhejta hai
        [y, mo, d, h, mi, s = 0] = ts;

    } else {

        const m = String(ts).match(
            /(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/
        );

        if (!m) {
            return NaN;
        }

        y = +m[1];
        mo = +m[2];
        d = +m[3];
        h = +m[4];
        mi = +m[5];
        s = m[6] ? +m[6] : 0;
    }

    return Date.UTC(y, mo - 1, d, h, mi, s) / 1000;
}


/* =====================================================
   EMA CALCULATION (SMA SEED, SAME AS TRADINGVIEW)
===================================================== */

function calculateEMA(prices, period) {

    const result = [];

    if (prices.length < period) {
        return result;
    }

    let sum = 0;

    for (let i = 0; i < period; i++) {
        sum += prices[i];
    }

    let ema = sum / period;

    result.push({ index: period - 1, value: ema });

    const multiplier = 2 / (period + 1);

    for (let i = period; i < prices.length; i++) {

        ema = (prices[i] - ema) * multiplier + ema;

        result.push({ index: i, value: ema });
    }

    return result;
}


/* EMA points ko chart format (time/value) me convert karo */
function toLineData(emaPoints, candleData) {

    return emaPoints.map(point => ({
        time: candleData[point.index].time,
        value: point.value
    }));
}


/* =====================================================
   VISIBLE RANGE: LAST N TRADING DAYS
===================================================== */

function showLastTradingDays(candleData, days) {

    if (candleData.length === 0) {
        return;
    }

    // time IST-as-UTC hai, isliye /86400 se trading date mil jati hai
    const dayKeys = [...new Set(candleData.map(c => Math.floor(c.time / 86400)))];

    const startDayKey = dayKeys[Math.max(0, dayKeys.length - days)];

    const firstVisible = candleData.find(
        c => Math.floor(c.time / 86400) >= startDayKey
    );

    const lastCandle = candleData[candleData.length - 1];

    chart.timeScale().setVisibleRange({
        from: firstVisible.time,
        to: lastCandle.time
    });
}


/* =====================================================
   LOAD CANDLE DATA
===================================================== */

async function loadChartData() {

    const select = document.getElementById("stockSelect");

    const instrumentKey = select.value;

    if (!instrumentKey) {
        alert("Please select a stock");
        return;
    }

    const symbol = select.options[select.selectedIndex].textContent;

    try {

        document.getElementById("loadBtn").textContent = "Loading...";


        /* ---------- GET 5 MINUTE CANDLES ---------- */

        const response = await fetch(
            "/api/nifty50/candles?instrumentKey=" +
            encodeURIComponent(instrumentKey)
        );

        if (!response.ok) {
            throw new Error("Candle API failed: " + response.status);
        }

        const candles = await response.json();

        if (!candles || candles.length === 0) {
            throw new Error("No candle data received");
        }

        console.log("Historical candles received:", candles.length);


        /* ---------- CONVERT ---------- */

        const chartData = candles
            .map(candle => ({
                time: toChartTime(candle.timestamp),
                open: Number(candle.open),
                high: Number(candle.high),
                low: Number(candle.low),
                close: Number(candle.close)
            }))
            .filter(c => Number.isFinite(c.time));


        /* ---------- SORT ---------- */

        chartData.sort((a, b) => a.time - b.time);


        /* ---------- REMOVE DUPLICATES ---------- */

        const uniqueChartData = [];
        const usedTimes = new Set();

        chartData.forEach(candle => {

            if (!usedTimes.has(candle.time)) {

                usedTimes.add(candle.time);
                uniqueChartData.push(candle);
            }
        });


        /* ---------- SET CANDLES ---------- */

        candleSeries.setData(uniqueChartData);


        /* ---------- EMA ON FULL HISTORY ---------- */

        const closePrices = uniqueChartData.map(c => c.close);

        const ema20 = calculateEMA(closePrices, 20);
        const ema50 = calculateEMA(closePrices, 50);
        const ema200 = calculateEMA(closePrices, 200);

        ema20Series.setData(toLineData(ema20, uniqueChartData));
        ema50Series.setData(toLineData(ema50, uniqueChartData));
        ema200Series.setData(toLineData(ema200, uniqueChartData));


        /* ---------- SHOW ONLY LAST 20 TRADING DAYS ---------- */

        showLastTradingDays(uniqueChartData, DISPLAY_TRADING_DAYS);


        /* ---------- UPDATE UI ---------- */

        document.getElementById("selectedStock").textContent = symbol;

        document.getElementById("chartTitle").textContent = symbol + " - 5 Minute";

        document.getElementById("candleCount").textContent = uniqueChartData.length;


        console.log("Stock:", symbol);
        console.log("5M Candles (total, used for EMA):", uniqueChartData.length);
        console.log("EMA20 points:", ema20.length);
        console.log("EMA50 points:", ema50.length);
        console.log("EMA200 points:", ema200.length);

        const lastCandle = candles[candles.length - 1];

        if (lastCandle) {
            console.log("Last Stock Candle Time:", lastCandle.timestamp);
        }

    } catch (error) {

        console.error("Chart data error:", error);

        alert("Candle data load nahi hua.");

    } finally {

        document.getElementById("loadBtn").textContent = "Load Chart";
    }
}


/* =====================================================
   START
===================================================== */

document.addEventListener("DOMContentLoaded", () => {

    initializeChart();

    loadStocks();

    document
        .getElementById("loadBtn")
        .addEventListener("click", loadChartData);
});

fetch("/api/nifty50/candles?instrumentKey=" + encodeURIComponent("NSE_EQ|INE090A01021"))
  .then(r => r.json())
  .then(c => {
    const perDay = {};
    c.forEach(x => {
      const d = String(x.timestamp).slice(0, 10);
      perDay[d] = (perDay[d] || 0) + 1;
    });
    console.table(perDay+" <-");
  });
