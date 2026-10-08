/* =====================================================
   LIQUIDITY WEBSOCKET
===================================================== */

let liquiditySocket = null;

function startLiquidityWebSocket() {

    liquiditySocket = new WebSocket(
        "ws://localhost:8080/ws/test"
    );

    liquiditySocket.onopen = function() {

        console.log("Liquidity WebSocket connected");

        document.getElementById("connectionText").innerText =
            "Connected";

        document.getElementById("connectionDot")
            .classList.add("connected");

        liquiditySocket.send("Hello Server");
    };

    liquiditySocket.onmessage = function(event) {

        try {

            const candle = JSON.parse(event.data);

            addCandleToLiquidityTable(candle);

        } catch (error) {

            console.log(
                "Liquidity JSON parse error:",
                error
            );
        }
    };

    liquiditySocket.onerror = function(error) {

        console.log(
            "Liquidity WebSocket error:",
            error
        );
    };

    liquiditySocket.onclose = function() {

        console.log(
            "Liquidity WebSocket disconnected"
        );

        document.getElementById(
            "connectionText"
        ).innerText = "Disconnected";

        document.getElementById(
            "connectionDot"
        ).classList.remove("connected");

        setTimeout(function() {

            startLiquidityWebSocket();

        }, 3000);
    };
}


/* =====================================================
   LIQUIDITY TABLE
===================================================== */

function addCandleToLiquidityTable(candle) {

    const tableBody =
        document.getElementById(
            "liquidityStockTableBody"
        );

    const tableWrapper =
        document.getElementById(
            "liquidityTableWrapper"
        );

    const stockName =
        getStockName(candle.instrumentKey);

    const rowId =
        "liquidity-" +
        candle.instrumentKey.replace(
            /[^a-zA-Z0-9_-]/g,
            "_"
        );

    let row =
        document.getElementById(rowId);

    if (!row) {

        row = document.createElement("tr");

        row.id = rowId;

        tableBody.appendChild(row);
    }

    row.innerHTML = `

        <td>
            ${Array.from(tableBody.children).indexOf(row) + 1}
        </td>

        <td>
            <strong>${stockName}</strong>
        </td>

        <td>
            ${candle.open ?? 0}
        </td>

        <td>
            ${candle.close ?? candle.currentPrice ?? 0}
        </td>

        <td>
            ${candle.low ?? 0}
        </td>

        <td>
            ${candle.high ?? 0}
        </td>

        <td>
            ${candle.volume ?? 0}
        </td>

        <td>
            ${candle.timestamp ?? 0}
        </td>

    `;

    tableWrapper.classList.remove("empty");

    if (tableBody.children.length > 10) {

        tableWrapper.classList.add("scrollable");

    } else {

        tableWrapper.classList.remove("scrollable");
    }

    document.getElementById(
        "liquidityTotalStocks"
    ).innerText =
        tableBody.children.length;
}


/* =====================================================
   EMA WEBSOCKET
===================================================== */

let emaSocket = null;

function startEmaWebSocket() {

    emaSocket = new WebSocket(
        "ws://localhost:8080/ws/ema"
    );

    emaSocket.onopen = function() {

        console.log(
            "EMA WebSocket connected"
        );
    };

    emaSocket.onmessage = function(event) {

        try {

            const emaData =
                JSON.parse(event.data);

            addEmaToTable(emaData);

        } catch (error) {

            console.log(
                "EMA JSON parse error:",
                error
            );
        }
    };

    emaSocket.onerror = function(error) {

        console.log(
            "EMA WebSocket error:",
            error
        );
    };

    emaSocket.onclose = function() {

        console.log(
            "EMA WebSocket disconnected"
        );

        setTimeout(function() {

            startEmaWebSocket();

        }, 3000);
    };
}


/* =====================================================
   EMA TABLE
===================================================== */

function addEmaToTable(ema) {

    const tableBody =
        document.getElementById(
            "emaStockTableBody"
        );

    const tableWrapper =
        document.getElementById(
            "emaTableWrapper"
        );

    const stockName =
        getStockName(ema.instrumentKey);

    const rowId =
        "ema-" +
        ema.instrumentKey.replace(
            /[^a-zA-Z0-9_-]/g,
            "_"
        );

    let row =
        document.getElementById(rowId);

    if (!row) {

        row = document.createElement("tr");

        row.id = rowId;

        tableBody.appendChild(row);
    }

    let alignment = "SIDEWAYS";

    if (ema.bullishAlignment) {

        alignment = "BULLISH";

    } else if (ema.bearishAlignment) {

        alignment = "BEARISH";
    }

    let trendClass = "";
    let trendfinal = "";

    if (ema.trend === "STRONG_BULLISH") {

        trendfinal = "bullish";

    } else if (ema.trend === "STRONG_BEARISH") {

        trendfinal = "bearish";
    }

    if (
        ema.trend === "STRONG_BULLISH" ||
        ema.trend === "BULLISH"
    ) {

        trendClass = "bullish";

    } else if (
        ema.trend === "STRONG_BEARISH" ||
        ema.trend === "BEARISH"
    ) {

        trendClass = "bearish";
    }

    row.innerHTML = `

        <td>
            ${Array.from(tableBody.children).indexOf(row) + 1}
        </td>

        <td>
            <strong>${stockName}</strong>
        </td>

        <td>
            ${Number(ema.currentPrice).toFixed(2)}
        </td>

        <td>
            ${Number(ema.ema20).toFixed(2)}
        </td>

        <td>
            ${Number(ema.ema50).toFixed(2)}
        </td>

        <td>
            ${Number(ema.ema200).toFixed(2)}
        </td>

        <td class="${trendClass}">
            ${ema.trend}
        </td>

        <td>
            ${Number(ema.bullishStrength).toFixed(0)}%
        </td>

        <td>
            ${Number(ema.bearishStrength).toFixed(0)}%
        </td>

        <td>
            ${ema.priceAboveEma20 ? "YES" : "NO"}
        </td>

        <td>
            ${ema.priceAboveEma50 ? "YES" : "NO"}
        </td>

        <td>
            ${ema.priceAboveEma200 ? "YES" : "NO"}
        </td>

        <td class="${trendfinal}">
            ${alignment}
        </td>
		
		<td>
		            ${ema.timestamp}
		        </td>

    `;

    tableWrapper.classList.remove("empty");

    if (tableBody.children.length > 10) {

        tableWrapper.classList.add("scrollable");

    } else {

        tableWrapper.classList.remove("scrollable");
    }

    document.getElementById(
        "totalStocks"
    ).innerText =
        tableBody.children.length;
}


/* =====================================================
   STOCK NAME
===================================================== */

function getStockName(instrumentKey) {

    const stocks = {
			
			"NSE_EQ|INE090A01021" : "ICICI BANK",
			        "NSE_EQ|INE062A01020" : "SBI",
			        "NSE_EQ|INE002A01018" : "RELIANCE",
			        "NSE_EQ|INE467B01029" : "TCS",
			        "NSE_EQ|INE009A01021" : "INFOSYS",
			        "NSE_EQ|INE397D01024" : "BHARTI AIRTEL",
			       "NSE_EQ|INE018A01030" : "LARSEN & TOUBRO",
			        "NSE_EQ|INE238A01034" : "AXIS BANK",
			        "NSE_EQ|INE040A01034" : "HDFC BANK"
    };

    return stocks[instrumentKey]
        || instrumentKey;
}


/* =====================================================
   TRADE HISTORY FROM DATABASE
===================================================== */

/* =====================================================
   TRADE HISTORY FROM DATABASE
===================================================== */

async function loadTradeHistory() {

    try {

        console.log(
            "Loading trade history from database..."
        );

        const response =
            await fetch("/api/ema/trades");


        if (!response.ok) {

            throw new Error(
                "Failed to load trade history. HTTP " +
                response.status
            );

        }


        const trades =
            await response.json();


        console.log(
            "TOTAL TRADES FROM DATABASE:",
            trades.length
        );


        const tableBody =
            document.getElementById(
                "tradeHistoryTableBody"
            );


        const tableWrapper =
            document.getElementById(
                "tradeHistoryTableWrapper"
            );


        const totalElement =
            document.getElementById(
                "tradeHistoryTotal"
            );


        /* =========================================
           SUMMARY ELEMENTS
        ========================================= */

        const totalInvestmentElement =
            document.getElementById(
                "totalInvestment"
            );





        const totalSellPriceElement =
            document.getElementById(
                "totalSellPrice"
            );

        const totalNetProfitElement =
            document.getElementById(
                "netProfit"
            );


        const totalLossElement =
            document.getElementById(
                "totalLoss"
            );


        const netProfitPercentElement =
            document.getElementById(
                "netProfitPercent"
            );


        /* =========================================
           CLEAR OLD TABLE
        ========================================= */

        tableBody.innerHTML = "";


        /* =========================================
           SUMMARY VARIABLES
        ========================================= */

        let totalBuyPrice = 0;

        let totalSellPrice = 0;

        let totalInvestment = 0;

        let totalNetProfit = 0;

        let totalProfit = 0;

        let totalLoss = 0;


        /* =========================================
           NO DATA
        ========================================= */

        if (
            !trades ||
            trades.length === 0
        ) {

            tableWrapper.classList.add(
                "empty"
            );

            tableWrapper.classList.remove(
                "scrollable"
            );


            totalElement.innerText = "0";


            totalInvestmentElement.innerText =
                "₹0.00";



            totalSellPriceElement.innerText =
                "₹0.00";

            totalLossElement.innerText =
                "₹0.00";

            netProfitPercentElement.innerText =
                "0.00%";


            return;

        }


        /* =========================================
           PROCESS ALL TRADES
        ========================================= */

        trades.forEach(
            function(trade, index) {

                const row =
                    document.createElement("tr");


                /* =================================
                   PRICE
                ================================= */



                const buyPrice =
                    Number(
                        trade.buyPrice ?? 0
                    );


                const sellPrice =
                    Number(
                        trade.sellPrice ?? 0
                    );

                totalNetProfit += (buyPrice - sellPrice);

                /* =================================
                   TOTAL BUY
                ================================= */

                if (buyPrice > 0) {

                    totalBuyPrice += buyPrice;

                }


                /* =================================
                   TOTAL SELL
                ================================= */

                if (sellPrice > 0) {

                    totalSellPrice += sellPrice;

                }


                /* =================================
                   INVESTMENT
                ================================= */

                if (buyPrice > 0) {

                    totalInvestment += buyPrice;

                }


                /* =================================
                   P/L
                ================================= */

                let changePercent = 0;

                let profitLoss = 0;


                if (
                    buyPrice > 0 &&
                    sellPrice > 0
                ) {

                    profitLoss =
                        buyPrice - sellPrice;


                    changePercent =
                        (
                            (
                                buyPrice - sellPrice
                            ) /
                            sellPrice
                        ) * 100;


                    /* =========================
                       PROFIT / LOSS TOTAL
                    ========================= */

                    if (profitLoss > 0) {

                        totalProfit +=
                            profitLoss;

                    }
                    else if (profitLoss < 0) {

                        totalLoss +=
                            Math.abs(profitLoss);

                    }

                }


                /* =================================
                   P/L CLASS
                ================================= */

                let plClass = "";


                if (profitLoss > 0) {

                    plClass = "bullish";

                }
                else if (profitLoss < 0) {

                    plClass = "bearish";

                }


                /* =================================
                   STATUS CLASS
                ================================= */

                let statusClass = "";


                if (
                    trade.status === "BUY"
                ) {

                    statusClass =
                        "bullish";

                }
                else if (
                    trade.status === "SELL"
                ) {

                    statusClass =
                        "bearish";

                }


                /* =================================
                   STOCK NAME
                ================================= */

                const stockName =
                    getStockName(
                        trade.instrumentKey
                    );


                /* =================================
                   CREATE ROW
                ================================= */

                row.innerHTML = `

                    <td>
                        ${index + 1}
                    </td>

                    <td>
                        <strong>
                            ${stockName}
                        </strong>
                    </td>

                    <td class="${statusClass}">
                        ${trade.status ?? "-"}
                    </td>

                    <td>
                        ${buyPrice > 0
                        ? buyPrice.toFixed(2)
                        : "-"
                    }
                    </td>
					
					<td>
                        ${trade.sellTime}
                    </td>

                    <td>
                        ${sellPrice > 0
                        ? sellPrice.toFixed(2)
                        : "-"
                    }
                    </td>
					
					<td>
                        ${trade.buyTime}
                    </td>

                    <td class="${plClass}">
                        ${changePercent.toFixed(2)}%
                    </td>

                    <td class="${plClass}">
                        ${profitLoss.toFixed(2)}
                    </td>

                `;


                tableBody.appendChild(row);

            }
        );


        /* =========================================
           NET PROFIT
        ========================================= */

        const netProfit =
            totalProfit - totalLoss;


        /* =========================================
           NET PROFIT %
           
           Formula:
           Net Profit / Total Investment × 100
        ========================================= */

        let netProfitPercent = 0;


        if (totalInvestment > 0) {

            netProfitPercent =
                (
                    netProfit /
                    totalInvestment
                ) * 100;

        }


        /* =========================================
           UPDATE SUMMARY
        ========================================= */

        totalInvestmentElement.innerText =
            "₹" +
            totalInvestment.toFixed(2);





        totalSellPriceElement.innerText =
            "₹" +
            totalSellPrice.toFixed(2);


        totalLossElement.innerText =
            "₹" +
            totalLoss.toFixed(2);

        totalNetProfitElement.innerText =
            "₹" +
            totalNetProfit.toFixed(2);


        netProfitPercentElement.innerText =
            netProfitPercent.toFixed(2) +
            "%";


        /* =========================================
           NET PROFIT COLOR
        ========================================= */

        if (netProfitPercent > 0) {

            netProfitPercentElement.style.color =
                "#22c55e";


            totalNetProfitElement.style.color =
                "#22c55e";

        }
        else if (netProfitPercent < 0) {

            netProfitPercentElement.style.color =
                "#ef4444";
				totalNetProfitElement.style.color =
				                "#ef4444";

        }
        else {

            netProfitPercentElement.style.color =
                "#ffffff";
				totalNetProfitElement.style.color =
								                "#ffffff";

        }


        /* =========================================
           SHOW TABLE
        ========================================= */

        tableWrapper.classList.remove(
            "empty"
        );


        /* =========================================
           SCROLL AFTER 10 ROWS
        ========================================= */

        if (trades.length > 10) {

            tableWrapper.classList.add(
                "scrollable"
            );

        }
        else {

            tableWrapper.classList.remove(
                "scrollable"
            );

        }


        /* =========================================
           TOTAL TRADES
        ========================================= */

        totalElement.innerText =
            trades.length;
    }
    catch (error) {

        console.error(
            "Trade History Load Error:",
            error
        );

    }

}


/* =====================================================
   START WEBSOCKETS
===================================================== */

startLiquidityWebSocket();

startEmaWebSocket();


/* =====================================================
   LOAD DATABASE TRADE HISTORY
===================================================== */

loadTradeHistory();