/* =====================================================
   SCALP TRADE HISTORY
   Source: GET /api/scalp/trades  (memory se, DB nahi)
   HTML same rahega, sirf ye JS replace karna hai.
===================================================== */

const TRADES_API = "/api/scalp/trades";
const REFRESH_MS = 3000;


/* =====================================================
   STOCK NAME
===================================================== */

function getStockName(instrumentKey) {
    const stocks = {
        "NSE_EQ|INE090A01021": "ICICI BANK",
        "NSE_EQ|INE062A01020": "SBI",
        "NSE_EQ|INE002A01018": "RELIANCE",
        "NSE_EQ|INE467B01029": "TCS",
        "NSE_EQ|INE009A01021": "INFOSYS",
        "NSE_EQ|INE397D01024": "BHARTI AIRTEL",
        "NSE_EQ|INE018A01030": "LARSEN & TOUBRO",
        "NSE_EQ|INE238A01034": "AXIS BANK",
        "NSE_EQ|INE040A01034": "HDFC BANK"
    };

    return stocks[instrumentKey] || instrumentKey || "-";
}


/* =====================================================
   HELPERS
===================================================== */

function formatTime(value) {
    if (!value) return "-";

    // Jackson kabhi array [y, m, d, h, mi, s] bhi bhej deta hai
    if (Array.isArray(value)) {
        const [y, mo, d, h = 0, mi = 0] = value;
        const p = n => String(n).padStart(2, "0");
        return `${y}-${p(mo)}-${p(d)} ${p(h)}:${p(mi)}`;
    }

    return String(value).replace("T", " ").substring(0, 16);
}

function setConnection(ok) {
    const dot = document.getElementById("connectionDot");
    const text = document.getElementById("connectionText");

    if (text) text.innerText = ok ? "Connected" : "Disconnected";
    if (dot) dot.style.backgroundColor = ok ? "#22c55e" : "#ef4444";
}

function money(value) {
    return "₹" + value.toFixed(2);
}

/* Exit reason ko saaf label me badlo: SL hit / Target hit / Trailing... */
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
    const sign = r > 0 ? "+" : "";
    return sign + r.toFixed(2) + "R";
}


/* =====================================================
   FETCH ALL SCALP TRADES
===================================================== */

async function fetchAllTrades() {
    const response = await fetch(TRADES_API);

    if (!response.ok) {
        throw new Error("HTTP error: " + response.status);
    }

    const trades = await response.json();

    if (!Array.isArray(trades)) {
        throw new Error("API response is not an array");
    }

    return trades;
}


/* =====================================================
   NORMALIZE: scalp trade -> buy/sell columns
   BUY  trade: buy  = entry, sell = exit
   SELL trade: sell = entry, buy  = exit
===================================================== */

function normalize(trade) {
    const isBuy = String(trade.side || "").toUpperCase() === "BUY";
    const isClosed = String(trade.status || "").toUpperCase() === "CLOSED";

    const entryPrice = Number(trade.entryPrice) || 0;
    const exitPrice = isClosed ? Number(trade.exitPrice) || 0 : 0;
    const qty = Number(trade.quantity) || 1;

    const entryTime = formatTime(trade.entryTime);
    const exitTime = isClosed ? formatTime(trade.exitTime) : "-";

    // P/L engine se aata hai (partial booking included), qty ke saath rupees me
    const profitLoss = isClosed ? Number(trade.pnlAmount) || 0 : 0;
    const pnlPerShare = isClosed ? Number(trade.pnlPerShare) || 0 : 0;
    const changePercent = isClosed && entryPrice > 0
        ? (pnlPerShare / entryPrice) * 100
        : 0;

    return {
        instrumentKey: trade.instrumentKey,
        side: isBuy ? "BUY" : "SELL",
        isClosed,
        qty,
        reason: trade.exitReason || (isClosed ? "" : "OPEN"),
        buyPrice: isBuy ? entryPrice : exitPrice,
        buyTime: isBuy ? entryTime : exitTime,
        sellPrice: isBuy ? exitPrice : entryPrice,
        sellTime: isBuy ? exitTime : entryTime,
        entryPrice,
        profitLoss,
        changePercent,
        reasonCode: trade.exitReason || "",
        rMultiple: isClosed ? Number(trade.rMultiple) || 0 : 0,
        partialBooked: !!trade.partialBooked,
        initialSl: Number(trade.initialStopLoss) || 0,
        currentSl: Number(trade.stopLoss) || 0,
        target1: Number(trade.target1) || 0,
        target2: Number(trade.target2) || 0
    };
}


/* =====================================================
   BUY TRADES
===================================================== */

async function loadBuyTrades(allTrades) {
    try {
        const trades = (allTrades || await fetchAllTrades())
            .filter(t => String(t.side || "").toUpperCase() === "BUY");

        renderSection("buy", trades);

    } catch (error) {
        console.error("BUY Trades Error:", error);
    }
}


/* =====================================================
   SELL TRADES
===================================================== */

async function loadSellTrades(allTrades) {
    try {
        const trades = (allTrades || await fetchAllTrades())
            .filter(t => String(t.side || "").toUpperCase() === "SELL");

        renderSection("sell", trades);

    } catch (error) {
        console.error("SELL Trades Error:", error);
    }
}


/* =====================================================
   COMMON RENDER (prefix = "buy" ya "sell")
===================================================== */

function renderSection(prefix, rawTrades) {
    const tableBody = document.getElementById(prefix + "TradeHistoryTableBody");
    const tableWrapper = document.getElementById(prefix + "TradeHistoryTableWrapper");

    const totalElement = document.getElementById(prefix + "TradeHistoryTotal");
    const investmentElement = document.getElementById(prefix + "TotalInvestment");
    const sellPriceElement = document.getElementById(prefix + "TotalSellPrice");
    const netProfitElement = document.getElementById(prefix + "NetProfit");
    const lossElement = document.getElementById(prefix + "TotalLoss");
    const profitPercentElement = document.getElementById(prefix + "NetProfitPercent");

    if (!tableBody || !tableWrapper || !totalElement ||
        !investmentElement || !sellPriceElement ||
        !netProfitElement || !lossElement || !profitPercentElement) {
        console.error(prefix.toUpperCase() + " section HTML elements are missing.");
        return;
    }

    const statusClass = prefix === "buy" ? "bullish" : "bearish";

    // naya trade upar dikhe
    const trades = rawTrades.map(normalize).reverse();

    tableBody.innerHTML = "";

    let totalInvestment = 0;
    let totalSellPrice = 0;
    let netProfit = 0;
    let totalLoss = 0;

    trades.forEach((t, index) => {

        totalInvestment += t.entryPrice * t.qty;

        if (t.sellPrice > 0) totalSellPrice += t.sellPrice * t.qty;

        if (t.isClosed) {
            netProfit += t.profitLoss;

            if (t.profitLoss < 0) {
                totalLoss += Math.abs(t.profitLoss);
            }
        }

        const plClass = t.profitLoss > 0
            ? "bullish"
            : t.profitLoss < 0
                ? "bearish"
                : "";

        const row = document.createElement("tr");

        // sell-side trade me pehle Sell columns, buy-side me pehle Buy columns (tumhare HTML jaisa)
        const firstPrice = prefix === "buy" ? t.buyPrice : t.sellPrice;
        const firstTime = prefix === "buy" ? t.buyTime : t.sellTime;
        const secondPrice = prefix === "buy" ? t.sellPrice : t.buyPrice;
        const secondTime = prefix === "buy" ? t.sellTime : t.buyTime;

        row.innerHTML = `
            <td>${index + 1}</td>
            <td><strong>${getStockName(t.instrumentKey)}</strong></td>
            <td class="${statusClass}" title="${t.reason}">${t.side}${t.isClosed ? "" : " (OPEN)"}</td>
            <td>${firstPrice > 0 ? firstPrice.toFixed(2) : "-"}</td>
            <td>${firstTime}</td>
            <td>${secondPrice > 0 ? secondPrice.toFixed(2) : "-"}</td>
            <td>${secondTime}</td>
            <td class="${plClass}">${t.isClosed ? t.changePercent.toFixed(2) + "%" : "-"}</td>
            <td class="${plClass}" title="Qty: ${t.qty} | ${t.reason}">${t.isClosed ? t.profitLoss.toFixed(2) : "-"}</td>
            <td class="${t.isClosed ? plClass : ""}" title="Initial SL: ${t.initialSl.toFixed(2)} | Current SL: ${t.currentSl.toFixed(2)} | TP1: ${t.target1.toFixed(2)} | TP2: ${t.target2.toFixed(2)}">
                ${t.isClosed ? reasonLabel(t.reasonCode) : "Running"}
                ${t.partialBooked ? '<br><small>50% booked at TP1</small>' : ""}
            </td>
            <td class="${plClass}">${t.isClosed ? formatR(t.rMultiple) : "-"}</td>
        `;

        tableBody.appendChild(row);
    });

    const profitPercent = totalInvestment > 0
        ? (netProfit / totalInvestment) * 100
        : 0;

    totalElement.innerText = trades.length;
    investmentElement.innerText = money(totalInvestment);
    sellPriceElement.innerText = money(totalSellPrice);
    netProfitElement.innerText = money(netProfit);
    lossElement.innerText = money(totalLoss);
    profitPercentElement.innerText = profitPercent.toFixed(2) + "%";

    const color = netProfit > 0
        ? "#22c55e"
        : netProfit < 0
            ? "#ef4444"
            : "#ffffff";

    netProfitElement.style.color = color;
    profitPercentElement.style.color = color;

    tableWrapper.classList.toggle("empty", trades.length === 0);
    tableWrapper.classList.toggle("scrollable", trades.length > 10);
}


/* =====================================================
   LOAD + AUTO REFRESH
===================================================== */

async function loadAll() {
    try {
        const trades = await fetchAllTrades();

        setConnection(true);

        await loadBuyTrades(trades);
        await loadSellTrades(trades);

    } catch (error) {
        setConnection(false);
        console.error("Scalp trades load error:", error);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadAll();
    setInterval(loadAll, REFRESH_MS);
});