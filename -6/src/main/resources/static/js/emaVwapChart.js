/* =====================================================
   SCALP LIVE CHART
   Data: ScalpController (memory se)
     /api/scalp/candles   -> candles + EMA9/21/50 + VWAP + RSI (incremental)
     /api/scalp/trades    -> BUY/SELL markers, SL/Target lines
     /api/scalp/summary   -> mode (REPLAY/LIVE), status, interval
===================================================== */

const API_STOCKS = "/api/nifty50/stocks";
const API_INSTRUMENTS = "/api/emavwap/instruments";
const API_CANDLES = "/api/emavwap/candles";
const API_TRADES = "/api/scalp/trades";
const API_SUMMARY = "/api/scalp/summary";

/* Kitne ms baad naya data poll karna hai (replay 1 sec/candle ke liye 1000) */
const POLL_MS = 1000;

let chart = null;

let candleSeries = null;
let volumeSeries = null;
let ema9Series = null;
let ema21Series = null;
let ema50Series = null;
let vwapSeries = null;

let markersPlugin = null;
let priceLines = [];
let priceLineSignature = "";

let currentKey = null;
let receivedCount = 0;
let lastCandleTime = 0;
let latestPoint = null;
let tradesCache = [];

let pollTimer = null;
let polling = false;
let loadToken = 0;


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
            borderColor: "#374151",
            scaleMargins: { top: 0.08, bottom: 0.25 }
        },

        timeScale: {
            borderColor: "#374151",
            timeVisible: true,
            secondsVisible: false,
            rightOffset: 6
        }
    });


    candleSeries = chart.addSeries(LightweightCharts.CandlestickSeries, {
        upColor: "#22c55e",
        downColor: "#ef4444",
        borderVisible: false,
        wickUpColor: "#22c55e",
        wickDownColor: "#ef4444"
    });

    ema9Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#3b82f6",
        lineWidth: 2,
        title: "EMA 9",
        priceLineVisible: false,
        lastValueVisible: true
    });

    ema21Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#f59e0b",
        lineWidth: 2,
        title: "EMA 21",
        priceLineVisible: false,
        lastValueVisible: true
    });

    ema50Series = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#a855f7",
        lineWidth: 2,
        title: "EMA 50",
        priceLineVisible: false,
        lastValueVisible: true
    });

    vwapSeries = chart.addSeries(LightweightCharts.LineSeries, {
        color: "#06b6d4",
        lineWidth: 2,
        lineStyle: LightweightCharts.LineStyle.Dashed,
        title: "VWAP",
        priceLineVisible: false,
        lastValueVisible: true
    });

    // volume niche alag scale par
    volumeSeries = chart.addSeries(LightweightCharts.HistogramSeries, {
        priceFormat: { type: "volume" },
        priceScaleId: "volume",
        priceLineVisible: false,
        lastValueVisible: false
    });

    chart.priceScale("volume").applyOptions({
        scaleMargins: { top: 0.82, bottom: 0 }
    });

    // BUY/SELL markers (lightweight-charts v5 plugin; v4 me setMarkers)
    if (typeof LightweightCharts.createSeriesMarkers === "function") {
        markersPlugin = LightweightCharts.createSeriesMarkers(candleSeries, []);
    }

    window.addEventListener("resize", () => {
        chart.applyOptions({ width: container.clientWidth });
    });
}


/* =====================================================
   TIME HELPER (TIMEZONE FIX) - tumhare chart jaisa hi
===================================================== */

function toChartTime(ts) {

    let y, mo, d, h, mi, s = 0;

    if (Array.isArray(ts)) {

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

function formatTime(ts) {

    if (!ts) return "-";

    if (Array.isArray(ts)) {
        const [y, mo, d, h = 0, mi = 0] = ts;
        const p = n => String(n).padStart(2, "0");
        return `${y}-${p(mo)}-${p(d)} ${p(h)}:${p(mi)}`;
    }

    return String(ts).replace("T", " ").substring(0, 16);
}


/* =====================================================
   HELPERS
===================================================== */

function setConnection(ok) {

    const dot = document.getElementById("connectionDot");
    const text = document.getElementById("connectionText");

    if (text) text.innerText = ok ? "Connected" : "Disconnected";
    if (dot) dot.style.backgroundColor = ok ? "#22c55e" : "#ef4444";
}

function setText(id, value, cls) {

    const el = document.getElementById(id);

    if (!el) return;

    el.textContent = value;

    if (cls !== undefined) {
        el.className = cls;
    }
}

function reasonLabel(code) {

    const labels = {
        "STOP_LOSS": "SL Hit",
        "TARGET": "Target Hit",
        "TRAILING_SL": "Trailing SL",
        "EMA9_EXIT": "EMA9 Exit",
        "VWAP_EXIT": "VWAP Exit",
        "TIME_STOP": "Time Stop",
        "EOD_SQUAREOFF": "EOD Square-off",
        "END_OF_DATA": "End of Data"
    };

    return labels[code] || code || "-";
}

function formatR(r) {
    return (r > 0 ? "+" : "") + r.toFixed(2) + "R";
}

function money(v) {
    return "₹" + v.toFixed(2);
}

async function getJson(url) {

    const response = await fetch(url);

    if (!response.ok) {
        throw new Error(url + " failed: " + response.status);
    }

    return response.json();
}


/* =====================================================
   LOAD STOCK LIST (tumhara /api/nifty50/stocks)
   Sirf wahi stocks jo scalp.instruments me hain
===================================================== */

async function loadStocks() {

    try {

        const stocks = await getJson(API_STOCKS);

        let allowed = null;

        try {
            const keys = await getJson(API_INSTRUMENTS);
            if (Array.isArray(keys) && keys.length > 0) {
                allowed = new Set(keys);
            }
        } catch (e) {
            console.warn("Scalp instruments API nahi mila, sab stocks dikha rahe hain");
        }

        const select = document.getElementById("stockSelect");

        select.innerHTML = '<option value="">Select Stock</option>';

        Object.entries(stocks).forEach(([instrumentKey, symbol]) => {

            if (allowed && !allowed.has(instrumentKey)) {
                return;
            }

            const option = document.createElement("option");

            option.value = instrumentKey;
            option.textContent = symbol;

            select.appendChild(option);
        });

        // pehla stock auto select + load => page khulte hi live chart
        if (select.options.length > 1) {
            select.selectedIndex = 1;
            loadChart();
        }

    } catch (error) {

        console.error("Stock loading error:", error);
    }
}


/* =====================================================
   CANDLE -> CHART DATA
===================================================== */

function candleOf(p) {

    return {
        time: toChartTime(p.timestamp),
        open: Number(p.open),
        high: Number(p.high),
        low: Number(p.low),
        close: Number(p.close)
    };
}

function volumeOf(p) {

    return {
        time: toChartTime(p.timestamp),
        value: Number(p.volume) || 0,
        color: Number(p.close) >= Number(p.open) ? "rgba(34,197,94,0.45)" : "rgba(239,68,68,0.45)"
    };
}

function lineOf(p, field) {

    if (p[field] === null || p[field] === undefined) {
        return null;
    }

    return { time: toChartTime(p.timestamp), value: Number(p[field]) };
}

/* Poora data ek saath set (sort + duplicate hata ke) */
function setAllData(points) {

    const sorted = points
        .filter(p => Number.isFinite(toChartTime(p.timestamp)))
        .sort((a, b) => toChartTime(a.timestamp) - toChartTime(b.timestamp));

    const unique = [];
    const used = new Set();

    sorted.forEach(p => {

        const t = toChartTime(p.timestamp);

        if (!used.has(t)) {
            used.add(t);
            unique.push(p);
        }
    });

    candleSeries.setData(unique.map(candleOf));
    volumeSeries.setData(unique.map(volumeOf));

    ema9Series.setData(unique.map(p => lineOf(p, "ema9")).filter(Boolean));
    ema21Series.setData(unique.map(p => lineOf(p, "ema21")).filter(Boolean));
    ema50Series.setData(unique.map(p => lineOf(p, "ema50")).filter(Boolean));
    vwapSeries.setData(unique.map(p => lineOf(p, "vwap")).filter(Boolean));

    if (unique.length > 0) {
        lastCandleTime = toChartTime(unique[unique.length - 1].timestamp);
        latestPoint = unique[unique.length - 1];
    }

    return unique.length;
}

/* Ek naya candle live add (live candle banta hua dikhega) */
function appendPoint(p) {

    const t = toChartTime(p.timestamp);

    if (!Number.isFinite(t) || t < lastCandleTime) {
        return;
    }

    lastCandleTime = t;
    latestPoint = p;

    candleSeries.update(candleOf(p));
    volumeSeries.update(volumeOf(p));

    const l9 = lineOf(p, "ema9");
    const l21 = lineOf(p, "ema21");
    const l50 = lineOf(p, "ema50");
    const lv = lineOf(p, "vwap");

    if (l9) ema9Series.update(l9);
    if (l21) ema21Series.update(l21);
    if (l50) ema50Series.update(l50);
    if (lv) vwapSeries.update(lv);
}


/* =====================================================
   MARKERS + SL/TARGET LINES (trades se)
===================================================== */

function setMarkers(markers) {

    if (markersPlugin) {
        markersPlugin.setMarkers(markers);
    } else if (typeof candleSeries.setMarkers === "function") {
        candleSeries.setMarkers(markers);
    }
}

function drawMarkers(trades) {

    const markers = [];

    trades.forEach(t => {

        const isBuy = t.side === "BUY";
        const entryTime = toChartTime(t.entryTime);

        if (Number.isFinite(entryTime)) {

            markers.push({
                time: entryTime,
                position: isBuy ? "belowBar" : "aboveBar",
                shape: isBuy ? "arrowUp" : "arrowDown",
                color: isBuy ? "#22c55e" : "#ef4444",
                text: (isBuy ? "BUY " : "SELL ") + Number(t.entryPrice).toFixed(2)
            });
        }

        if (t.status === "CLOSED") {

            const exitTime = toChartTime(t.exitTime);

            if (Number.isFinite(exitTime)) {

                const win = Number(t.pnlPerShare) > 0;

                markers.push({
                    time: exitTime,
                    position: isBuy ? "aboveBar" : "belowBar",
                    shape: "circle",
                    color: win ? "#22c55e" : "#ef4444",
                    text: reasonLabel(t.exitReason) + " " + formatR(Number(t.rMultiple) || 0)
                        + (t.partialBooked ? " (P)" : "")
                });
            }
        }
    });

    markers.sort((a, b) => a.time - b.time);

    setMarkers(markers);
}

function clearPriceLines() {

    priceLines.forEach(line => {
        try { candleSeries.removePriceLine(line); } catch (e) { /* ignore */ }
    });

    priceLines = [];
    priceLineSignature = "";
}

/* Open trade ka entry / SL / TP1 / TP2 chart par horizontal lines */
function drawPriceLines(openTrade) {

    if (!openTrade) {
        clearPriceLines();
        return;
    }

    const signature = [openTrade.id, openTrade.stopLoss, openTrade.partialBooked].join("|");

    if (signature === priceLineSignature) {
        return;
    }

    clearPriceLines();

    priceLineSignature = signature;

    const add = (price, color, title, style) => {

        priceLines.push(candleSeries.createPriceLine({
            price: Number(price),
            color: color,
            lineWidth: 1,
            lineStyle: style,
            axisLabelVisible: true,
            title: title
        }));
    };

    const LS = LightweightCharts.LineStyle;

    add(openTrade.entryPrice, "#9ca3af", "Entry", LS.Solid);
    add(openTrade.stopLoss, "#ef4444", openTrade.partialBooked ? "Trail SL" : "SL", LS.Dashed);

    if (!openTrade.partialBooked) {
        add(openTrade.target1, "#22c55e", "TP1", LS.Dotted);
    }

    add(openTrade.target2, "#16a34a", "TP2", LS.Dashed);
}


/* =====================================================
   INFO PANEL + TRADES TABLE
===================================================== */

function updateTrend(p) {

    if (!p || p.ema9 == null || p.ema21 == null || p.ema50 == null || p.vwap == null) {
        setText("trendText", "-", "flat");
        return;
    }

    const bull = p.close > p.vwap && p.ema9 > p.ema21 && p.ema21 > p.ema50;
    const bear = p.close < p.vwap && p.ema9 < p.ema21 && p.ema21 < p.ema50;

    if (bull) setText("trendText", "BULLISH", "up");
    else if (bear) setText("trendText", "BEARISH", "down");
    else setText("trendText", "SIDEWAYS", "flat");
}

function updateInfo(trades) {

    const p = latestPoint;

    if (p) {

        setText("lastPrice", Number(p.close).toFixed(2));
        setText("rsiText", p.rsi == null ? "-" : Number(p.rsi).toFixed(1));
        updateTrend(p);
    }

    const open = trades.find(t => t.status === "OPEN");

    if (open && p) {

        const risk = Math.abs(open.entryPrice - open.initialStopLoss);
        const dir = open.side === "BUY" ? 1 : -1;
        const unreal = risk > 0 ? dir * (p.close - open.entryPrice) / risk : 0;

        setText("positionText",
            `${open.side} @ ${Number(open.entryPrice).toFixed(2)} | SL ${Number(open.stopLoss).toFixed(2)} | ${formatR(unreal)}`,
            dir === 1 ? "up" : "down");

    } else {

        setText("positionText", "No open trade", "flat");
    }

    const pnl = trades
        .filter(t => t.status === "CLOSED")
        .reduce((sum, t) => sum + (Number(t.pnlAmount) || 0), 0);

    setText("stockPnl", money(pnl), pnl > 0 ? "up" : pnl < 0 ? "down" : "flat");
}

function renderTradesTable(trades) {

    const body = document.getElementById("tradesBody");

    body.innerHTML = "";

    trades.slice().reverse().forEach((t, i) => {

        const closed = t.status === "CLOSED";
        const pnl = closed ? Number(t.pnlAmount) || 0 : 0;
        const cls = pnl > 0 ? "bullish" : pnl < 0 ? "bearish" : "";

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${trades.length - i}</td>
            <td class="${t.side === "BUY" ? "bullish" : "bearish"}">${t.side}${closed ? "" : " (OPEN)"}</td>
            <td>${formatTime(t.entryTime)}</td>
            <td>${Number(t.entryPrice).toFixed(2)}</td>
            <td>${Number(t.initialStopLoss).toFixed(2)}</td>
            <td>${Number(t.target1).toFixed(2)}</td>
            <td>${Number(t.target2).toFixed(2)}</td>
            <td>${closed ? formatTime(t.exitTime) : "-"}</td>
            <td>${closed ? Number(t.exitPrice).toFixed(2) : "-"}</td>
            <td class="${cls}">${closed ? reasonLabel(t.exitReason) : "Running"}${t.partialBooked ? "<br><small>50% booked at TP1</small>" : ""}</td>
            <td class="${cls}">${closed ? formatR(Number(t.rMultiple) || 0) : "-"}</td>
            <td class="${cls}">${closed ? money(pnl) : "-"}</td>
        `;

        body.appendChild(row);
    });
}

async function refreshTrades() {

    const all = await getJson(API_TRADES);

    tradesCache = all.filter(t => t.instrumentKey === currentKey);

    drawMarkers(tradesCache);
    drawPriceLines(tradesCache.find(t => t.status === "OPEN"));
    updateInfo(tradesCache);
    renderTradesTable(tradesCache);
}

async function refreshSummary() {

    const s = await getJson(API_SUMMARY);

    setText("modeText", `${s.mode} • ${s.status}`);

    if (s.intervalMinutes) {
        setText("timeframeText", s.intervalMinutes + " Minutes");
    }
}


/* =====================================================
   LOAD CHART (pehli baar / stock change)
===================================================== */

function stopPolling() {

    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

async function loadChart() {

    const select = document.getElementById("stockSelect");

    const instrumentKey = select.value;

    if (!instrumentKey) {
        alert("Please select a stock");
        return;
    }

    const symbol = select.options[select.selectedIndex].textContent;

    const token = ++loadToken;

    stopPolling();

    const btn = document.getElementById("loadBtn");

    try {

        btn.textContent = "Loading...";

        currentKey = instrumentKey;
        receivedCount = 0;
        lastCandleTime = 0;
        latestPoint = null;

        clearPriceLines();
        setMarkers([]);

        const res = await getJson(`${API_CANDLES}?instrumentKey=${encodeURIComponent(instrumentKey)}&from=0`);

        if (token !== loadToken) {
            return;
        }

        const count = setAllData(res.candles || []);

        receivedCount = res.total || 0;

        chart.timeScale().fitContent();

        setText("selectedStock", symbol);
        setText("chartTitle", symbol + (count === 0 ? " - candles abhi aaye nahi (replay/warm-up chal raha hai)" : " - Scalp Live"));
        setText("candleCount", String(receivedCount));

        setConnection(true);

        await refreshSummary();
        await refreshTrades();

        pollTimer = setInterval(pollOnce, POLL_MS);

    } catch (error) {

        console.error("Chart load error:", error);

        setConnection(false);

        alert("Scalp data load nahi hua. /api/scalp/candles check karo (scalp.enabled=true?).");

    } finally {

        btn.textContent = "Load Chart";
    }
}


/* =====================================================
   LIVE POLL: naye candles aate hi chart me jud jate hain
===================================================== */

async function pollOnce() {

    if (polling || !currentKey) {
        return;
    }

    polling = true;

    const token = loadToken;

    try {

        const res = await getJson(
            `${API_CANDLES}?instrumentKey=${encodeURIComponent(currentKey)}&from=${receivedCount}`
        );

        if (token !== loadToken) {
            return;
        }

        // backend restart hua (candles kam ho gaye) => chart dobara load
        if (res.total < receivedCount) {
            polling = false;
            loadChart();
            return;
        }

        (res.candles || []).forEach(appendPoint);

        receivedCount = res.total;

        setText("candleCount", String(receivedCount));

        await refreshTrades();
        await refreshSummary();

        setConnection(true);

    } catch (error) {

        console.error("Poll error:", error);

        setConnection(false);

    } finally {

        polling = false;
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
        .addEventListener("click", loadChart);

    document
        .getElementById("stockSelect")
        .addEventListener("change", loadChart);
});