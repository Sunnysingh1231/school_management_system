
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
   FETCH ALL TRADES
===================================================== */

async function fetchAllTrades() {
    const response = await fetch("/api/ema/trades");

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
   BUY TRADES FUNCTION
===================================================== */

async function loadBuyTrades() {
    try {
        console.log("Loading BUY trades...");

        const trades = await fetchAllTrades();

        const buyTrades = trades.filter(
            trade => String(trade.status || "").toUpperCase() === "BUY"
        );

        console.log("Total BUY trades:", buyTrades.length);

        renderBuyTrades(buyTrades);

    } catch (error) {
        console.error("BUY Trades Error:", error);
    }
}


/* =====================================================
   SELL TRADES FUNCTION
===================================================== */

async function loadSellTrades() {
    try {
        console.log("Loading SELL trades...");

        const trades = await fetchAllTrades();

        const sellTrades = trades.filter(
            trade => String(trade.status || "").toUpperCase() === "SELL"
        );

        console.log("Total SELL trades:", sellTrades.length);

        renderSellTrades(sellTrades);

    } catch (error) {
        console.error("SELL Trades Error:", error);
    }
}


/* =====================================================
   BUY SECTION RENDER
===================================================== */

function renderBuyTrades(trades) {
    const tableBody = document.getElementById("buyTradeHistoryTableBody");
    const tableWrapper = document.getElementById("buyTradeHistoryTableWrapper");

    const totalElement = document.getElementById("buyTradeHistoryTotal");
    const investmentElement = document.getElementById("buyTotalInvestment");
    const sellPriceElement = document.getElementById("buyTotalSellPrice");
    const netProfitElement = document.getElementById("buyNetProfit");
    const lossElement = document.getElementById("buyTotalLoss");
    const profitPercentElement = document.getElementById("buyNetProfitPercent");

    if (!tableBody || !tableWrapper || !totalElement ||
        !investmentElement || !sellPriceElement ||
        !netProfitElement || !lossElement || !profitPercentElement) {
        console.error("BUY section HTML elements are missing.");
        return;
    }

    tableBody.innerHTML = "";

    let totalInvestment = 0;
    let totalSellPrice = 0;
    let netProfit = 0;
    let totalLoss = 0;

    trades.forEach((trade, index) => {
        const buyPrice = Number(trade.buyPrice) || 0;
        const sellPrice = Number(trade.sellPrice) || 0;

        const isClosed = buyPrice > 0 && sellPrice > 0;
        const profitLoss = isClosed ? sellPrice - buyPrice : 0;
        const changePercent = isClosed
            ? (profitLoss / buyPrice) * 100
            : 0;

        if (buyPrice > 0) totalInvestment += buyPrice;
        if (sellPrice > 0) totalSellPrice += sellPrice;

        if (isClosed && profitLoss < 0) {
            totalLoss += Math.abs(profitLoss);
        }

        if (isClosed) {
            netProfit += profitLoss;
        }

        const plClass = profitLoss > 0
            ? "bullish"
            : profitLoss < 0
                ? "bearish"
                : "";

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${index + 1}</td>
			
            <td><strong>${getStockName(trade.instrumentKey)}</strong></td>
			
            <td class="bullish">${trade.status ?? "-"}</td>
			
            <td>${buyPrice > 0 ? buyPrice.toFixed(2) : "-"}</td>
			
            <td>${trade.buyTime ?? "-"}</td>
			
            <td>${sellPrice > 0 ? sellPrice.toFixed(2) : "-"}</td>
			
            <td>${trade.sellTime ?? "-"}</td>
			
            <td class="${plClass}">
                ${isClosed ? changePercent.toFixed(2) + "%" : "-"}
            </td>
			
            <td class="${plClass}">
                ${isClosed ? profitLoss.toFixed(2) : "-"}
            </td>
        `;

        tableBody.appendChild(row);
    });

    const profitPercent = totalInvestment > 0
        ? (netProfit / totalInvestment) * 100
        : 0;

    totalElement.innerText = trades.length;
    investmentElement.innerText = "₹" + totalInvestment.toFixed(2);
    sellPriceElement.innerText = "₹" + totalSellPrice.toFixed(2);
    netProfitElement.innerText = "₹" + netProfit.toFixed(2) + " / " + (netProfit + totalLoss).toFixed(2);
    lossElement.innerText = "₹" + totalLoss.toFixed(2) + " / " + (netProfit + totalLoss).toFixed(2);
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
   SELL SECTION RENDER
===================================================== */

function renderSellTrades(trades) {
    const tableBody = document.getElementById("sellTradeHistoryTableBody");
    const tableWrapper = document.getElementById("sellTradeHistoryTableWrapper");

    const totalElement = document.getElementById("sellTradeHistoryTotal");
    const investmentElement = document.getElementById("sellTotalInvestment");
    const sellPriceElement = document.getElementById("sellTotalSellPrice");
    const netProfitElement = document.getElementById("sellNetProfit");
    const lossElement = document.getElementById("sellTotalLoss");
    const profitPercentElement = document.getElementById("sellNetProfitPercent");

    if (!tableBody || !tableWrapper || !totalElement ||
        !investmentElement || !sellPriceElement ||
        !netProfitElement || !lossElement || !profitPercentElement) {
        console.error("SELL section HTML elements are missing.");
        return;
    }

    tableBody.innerHTML = "";

    let totalInvestment = 0;
    let totalSellPrice = 0;
    let netProfit = 0;
    let totalLoss = 0;

    trades.forEach((trade, index) => {
        const buyPrice = Number(trade.buyPrice) || 0;
        const sellPrice = Number(trade.sellPrice) || 0;

        const isClosed = buyPrice > 0 && sellPrice > 0;
        const profitLoss = isClosed ? sellPrice - buyPrice : 0;
        const changePercent = isClosed
            ? (profitLoss / buyPrice) * 100
            : 0;

        if (buyPrice > 0) totalInvestment += buyPrice;
        if (sellPrice > 0) totalSellPrice += sellPrice;

        if (isClosed && profitLoss < 0) {
            totalLoss += Math.abs(profitLoss);
        }

        if (isClosed) {
            netProfit += profitLoss;
        }

        const plClass = profitLoss > 0
            ? "bullish"
            : profitLoss < 0
                ? "bearish"
                : "";

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${index + 1}</td>
            <td><strong>${getStockName(trade.instrumentKey)}</strong></td>
            <td class="bearish">${trade.status ?? "-"}</td>
            <td>${sellPrice > 0 ? sellPrice.toFixed(2) : "-"}</td>
            <td>${trade.sellTime ?? "-"}</td>
            <td>${buyPrice > 0 ? buyPrice.toFixed(2) : "-"}</td>
            <td>${trade.buyTime ?? "-"}</td>
            <td class="${plClass}">
                ${isClosed ? changePercent.toFixed(2) + "%" : "-"}
            </td>
            <td class="${plClass}">
                ${isClosed ? profitLoss.toFixed(2) : "-"}
            </td>
        `;

        tableBody.appendChild(row);
    });

    const profitPercent = totalInvestment > 0
        ? (netProfit / totalInvestment) * 100
        : 0;

    totalElement.innerText = trades.length;
    investmentElement.innerText = "₹" + totalInvestment.toFixed(2);
    sellPriceElement.innerText = "₹" + totalSellPrice.toFixed(2);
    netProfitElement.innerText = "₹" + netProfit.toFixed(2);
    lossElement.innerText = "₹" + totalLoss.toFixed(2);
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
   INITIAL LOAD
===================================================== */

document.addEventListener("DOMContentLoaded", () => {
    loadBuyTrades();
    loadSellTrades();
});

