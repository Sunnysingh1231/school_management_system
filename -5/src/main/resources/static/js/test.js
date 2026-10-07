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

            //console.log("Liquidity Object:", candle);

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


    /*
     * instrumentKey me | hota hai.
     * HTML id me | use karne se problem aa sakti hai.
     */

    const rowId =
        "liquidity-" +
        candle.instrumentKey.replace(/[^a-zA-Z0-9_-]/g, "_");


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

        console.log("EMA WebSocket connected");

    };


    emaSocket.onmessage = function(event) {

        try {

            const emaData =
                JSON.parse(event.data);


           // console.log(
           //     "EMA Object:",
           //     emaData
           // );


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


    /*
     * Safe row ID
     */

    const rowId =
        "ema-" +
        ema.instrumentKey.replace(
            /[^a-zA-Z0-9_-]/g,
            "_"
        );


    let row =
        document.getElementById(rowId);


    /*
     * First time stock aaye
     * to row create karo
     */

    if (!row) {

        row = document.createElement("tr");

        row.id = rowId;

        tableBody.appendChild(row);

    }


    /*
     * Alignment
     */

    let alignment = "SIDEWAYS";


    if (ema.bullishAlignment) {

        alignment = "BULLISH";

    } else if (ema.bearishAlignment) {

        alignment = "BEARISH";

    }


    /*
     * Trend class
     */

    let trendClass = "";
    let trendfinal = "";


    if (
        ema.trend === "STRONG_BULLISH"
    ) {

        trendfinal = "bullish";

    } else if (
        ema.trend === "STRONG_BEARISH"
    ) {

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


    /*
     * Update row
     */

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

    `;


    tableWrapper.classList.remove("empty");


    /*
     * Total EMA stocks
     */

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

        "NSE_EQ|INE040A01034":
            "HDFC BANK",

        "NSE_EQ|INE009A01021":
            "INFOSYS",

        "NSE_EQ|INE467B01029":
            "TCS",

        "NSE_EQ|INE090A01021":
            "ICICI BANK",

        "NSE_EQ|INE062A01020":
            "SBIN"

    };


    return stocks[instrumentKey]
        || instrumentKey;

}

/* =====================================================
   BUY / SELL TRADE WEBSOCKET
===================================================== */

let emaBuySellSocket = null;


function startEmaBSWebSocket() {

    emaBuySellSocket = new WebSocket(
        "ws://localhost:8080/ws/bs"
    );


    /*
     * =================================================
     * CONNECTED
     * =================================================
     */

    emaBuySellSocket.onopen = function() {

        console.log(
            "Buy Sell EMA WebSocket connected"
        );

        emaBuySellSocket.send(
            "Hello EMA Buy Sell Server"
        );
    };


    /*
     * =================================================
     * MESSAGE
     * =================================================
     */

    emaBuySellSocket.onmessage = function(event) {

        try {

            const trade =
                JSON.parse(event.data);


            console.log(
                "TRADE OBJECT:",
                trade
            );


            addTradeToHistory(trade);


        } catch (error) {

            console.log(
                "Buy Sell JSON parse error:",
                error
            );
        }
    };


    /*
     * =================================================
     * ERROR
     * =================================================
     */

    emaBuySellSocket.onerror = function(error) {

        console.log(
            "Buy Sell EMA WebSocket error:",
            error
        );
    };


    /*
     * =================================================
     * CLOSE
     * =================================================
     */

    emaBuySellSocket.onclose = function() {

        console.log(
            "Buy Sell WebSocket disconnected"
        );


        setTimeout(function() {

            startEmaBSWebSocket();

        }, 3000);
    };
}

/* =====================================================
   TRADE HISTORY
===================================================== */

function addTradeToHistory(trade) {

    const tableBody =
        document.getElementById(
            "tradeHistoryTableBody"
        );


    const tableWrapper =
        document.getElementById(
            "tradeHistoryTableWrapper"
        );


    if (!tableBody || !tableWrapper) {
        return;
    }


    const stockName = trade.instrumentKey ?? "-";


    /*
     * =================================================
     * SAFE ROW ID
     * =================================================
     */

    const rowId =
        "trade-" +
        trade.instrumentKey.replace(
            /[^a-zA-Z0-9\_-]/g,
            "_"
        );


    let row =
        document.getElementById(rowId);


    /*
     * =================================================
     * CREATE ROW
     * =================================================
     */

    if (!row) {

        row = document.createElement("tr");

        row.id = rowId;

        tableBody.appendChild(row);
    }


    /*
     * =================================================
     * STATUS CLASS
     * =================================================
     */

    let statusClass = "";

    if (trade.status === "BUY") {

        statusClass = "buy";

    } else if (trade.status === "SELL") {

        statusClass = "sell";
    }


    /*
     * =================================================
     * TREND CLASS
     * =================================================
     */

    let trendClass = "";

    if (
        trade.trend === "STRONG_BULLISH" ||
        trade.trend === "BULLISH"
    ) {

        trendClass = "bullish";

    } else if (
        trade.trend === "STRONG_BEARISH" ||
        trade.trend === "BEARISH"
    ) {

        trendClass = "bearish";

    } else {

        trendClass = "neutral";
    }


    /*
     * =================================================
     * FORMAT VALUES
     * =================================================
     */

    const buyPrice =
        trade.buyPrice != null
            ? Number(trade.buyPrice).toFixed(2)
            : "-";


    const sellPrice =
        trade.sellPrice != null
            ? Number(trade.sellPrice).toFixed(2)
            : "-";


    const changePercentage =
        trade.changePercentage != null
            ? Number(
                trade.changePercentage
            ).toFixed(2) + "%"
            : "-";


    const profitLoss =
        trade.profitLoss != null
            ? Number(
                trade.profitLoss
            ).toFixed(2)
            : "-";


    const bullishStrength =
        trade.bullishStrength != null
            ? Number(
                trade.bullishStrength
            ).toFixed(0) + "%"
            : "-";


    const bearishStrength =
        trade.bearishStrength != null
            ? Number(
                trade.bearishStrength
            ).toFixed(0) + "%"
            : "-";


    const ema20 =
        trade.ema20 != null
            ? Number(
                trade.ema20
            ).toFixed(2)
            : "-";


    const ema50 =
        trade.ema50 != null
            ? Number(
                trade.ema50
            ).toFixed(2)
            : "-";


    const ema200 =
        trade.ema200 != null
            ? Number(
                trade.ema200
            ).toFixed(2)
            : "-";


    /*
     * =================================================
     * UPDATE ROW
     * =================================================
     */

    row.innerHTML = `

        <td>
            ${
                Array.from(
                    tableBody.children
                ).indexOf(row) + 1
            }
        </td>


        <td>
            <strong>
                ${stockName}
            </strong>
        </td>


        <td class="signal ${statusClass}">
            ${trade.status ?? "-"}
        </td>


        <td>
            ${buyPrice}
        </td>


        <td>
            ${sellPrice}
        </td>


        <td class="${
            trade.changePercentage > 0
                ? "bullish"
                : trade.changePercentage < 0
                    ? "bearish"
                    : "neutral"
        }">

            ${changePercentage}

        </td>


        <td class="${
            trade.profitLoss > 0
                ? "bullish"
                : trade.profitLoss < 0
                    ? "bearish"
                    : "neutral"
        }">

            ${profitLoss}

        </td>


        <td>
            ${bullishStrength}
        </td>


        <td>
            ${bearishStrength}
        </td>


        <td>
            ${ema20}
        </td>


        <td>
            ${ema50}
        </td>


        <td>
            ${ema200}
        </td>


        <td class="${trendClass}">
            ${trade.trend ?? "-"}
        </td>


        <td>
            ${
                trade.buyTime
                    ? formatTradeTime(
                        trade.buyTime
                    )
                    : "-"
            }
        </td>


        <td>
            ${
                trade.sellTime
                    ? formatTradeTime(
                        trade.sellTime
                    )
                    : "-"
            }
        </td>

    `;


    /*
     * =================================================
     * SHOW TABLE
     * =================================================
     */

    tableWrapper.classList.remove(
        "empty"
    );


    /*
     * =================================================
     * STOCK COUNT
     * =================================================
     */

    document.getElementById(
        "tradeHistoryTotal"
    ).innerText =
        tableBody.children.length;
}


/* =====================================================
   START BOTH WEBSOCKETS
===================================================== */

startLiquidityWebSocket();

startEmaWebSocket();

startEmaBSWebSocket();