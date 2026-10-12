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

startLiquidityWebSocket();

startEmaWebSocket();
