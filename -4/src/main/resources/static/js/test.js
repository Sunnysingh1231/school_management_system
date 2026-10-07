let historicalLiveSocket = null;

/* =========================================================
   LIVE DATA STORAGE
========================================================= */

const liveStocks = {};


/* =========================================================
   CONNECT HISTORICAL LIVE WEBSOCKET
========================================================= */

function connectHistoricalLiveWebSocket() {

    const protocol =
        window.location.protocol === "https:"
            ? "wss://"
            : "ws://";

    const wsUrl =
        protocol +
        window.location.host +
        "/ws/historical-live";

    console.log("Connecting:", wsUrl);

    historicalLiveSocket = new WebSocket(wsUrl);


    /* =====================================================
       SOCKET OPEN
    ===================================================== */

    historicalLiveSocket.onopen = function () {

        console.log("=================================");
        console.log("HISTORICAL LIVE SOCKET CONNECTED");
        console.log("=================================");

        updateConnectionStatus(true);
    };


    /* =====================================================
       LIVE DATA RECEIVED
    ===================================================== */

    historicalLiveSocket.onmessage = function (event) {

        try {

            const data = JSON.parse(event.data);

            console.log("LIVE DATA:", data);


            /*
             * Browser connect hone par backend
             * Map<String, MarketTick> bhejta hai.
             *
             * Example:
             *
             * {
             *   "NSE_EQ|INE040A01034": {...}
             * }
             */

            if (
                data &&
                typeof data === "object" &&
                !Array.isArray(data)
            ) {

                /*
                 * Agar ye single MarketTick hai
                 */
                if (data.instrumentKey) {

                    liveStocks[data.instrumentKey] = data;

                }

                /*
                 * Agar ye Map<String, MarketTick> hai
                 */
                else {

                    Object.keys(data).forEach(function (key) {

                        const tick = data[key];

                        if (
                            tick &&
                            tick.instrumentKey
                        ) {

                            liveStocks[
                                tick.instrumentKey
                            ] = tick;
                        }

                    });
                }


                /*
                 * HTML tables update
                 */
                updateLiquidityTable();
                updateEmaTable();
            }

        } catch (error) {

            console.error(
                "ERROR PARSING LIVE DATA:",
                error
            );

            console.log(
                "RAW DATA:",
                event.data
            );
        }
    };


    /* =====================================================
       SOCKET ERROR
    ===================================================== */

    historicalLiveSocket.onerror = function (error) {

        console.error(
            "HISTORICAL LIVE SOCKET ERROR:",
            error
        );

        updateConnectionStatus(false);
    };


    /* =====================================================
       SOCKET CLOSE
    ===================================================== */

    historicalLiveSocket.onclose = function () {

        console.log(
            "HISTORICAL LIVE SOCKET CLOSED"
        );

        updateConnectionStatus(false);

        setTimeout(
            connectHistoricalLiveWebSocket,
            3000
        );
    };
}


/* =========================================================
   CONNECTION STATUS
========================================================= */

function updateConnectionStatus(connected) {

    const dot =
        document.getElementById("connectionDot");

    const text =
        document.getElementById("connectionText");


    if (!dot || !text) {
        return;
    }


    if (connected) {

        dot.classList.add("connected");

        text.textContent =
            "Live Connected";

    } else {

        dot.classList.remove("connected");

        text.textContent =
            "Disconnected";
    }
}


/* =========================================================
   LIQUIDITY TABLE
========================================================= */

function updateLiquidityTable() {

    const tbody =
        document.getElementById(
            "liquidityStockTableBody"
        );

    if (!tbody) {
        return;
    }


    tbody.innerHTML = "";


    const stocks =
        Object.values(liveStocks);


    document.getElementById(
        "liquidityTotalStocks"
    ).textContent = stocks.length;


    stocks.forEach(function (stock, index) {

        const row =
            document.createElement("tr");


        const ltp =
            number(stock.ltp);


        const changePercent =
            number(stock.changePercent);


        const volume =
            number(stock.volume);


        /*
         * Historical data mein bid/ask available
         * nahi hai, isliye 0 / N/A handle kar rahe hain.
         */

        const bestBid =
            getBestBid(stock);


        const bestAsk =
            getBestAsk(stock);


        const spread =
            bestBid !== null &&
            bestAsk !== null
                ? bestAsk - bestBid
                : null;


        const spreadPercent =
            spread !== null &&
            bestBid !== 0
                ? (spread / bestBid) * 100
                : null;


        const tradedValue =
            volume * ltp;


        const rowNumber =
            index + 1;


        row.innerHTML = `

            <td>${rowNumber}</td>

            <td>
                <strong>
                    ${getSymbol(stock)}
                </strong>
            </td>

            <td>${formatNumber(ltp)}</td>

            <td class="${changePercent >= 0 ? "positive" : "negative"}">
                ${formatNumber(changePercent)}%
            </td>

            <td>
                ${formatVolumeCr(volume)}
            </td>

            <td>
                ${formatNumber(tradedValue / 10000000)}
            </td>

            <td>
                ${bestBid !== null
                    ? formatNumber(bestBid)
                    : "N/A"}
            </td>

            <td>
                ${bestAsk !== null
                    ? formatNumber(bestAsk)
                    : "N/A"}
            </td>

            <td>
                ${spread !== null
                    ? formatNumber(spread)
                    : "N/A"}
            </td>

            <td>
                ${spreadPercent !== null
                    ? formatNumber(spreadPercent) + "%"
                    : "N/A"}
            </td>

            <td>N/A</td>

            <td>N/A</td>

            <td>
                <strong class="live-safe">
                    LIVE
                </strong>
            </td>
        `;


        tbody.appendChild(row);

    });


    /*
     * Empty class remove
     */

    const wrapper =
        document.getElementById(
            "liquidityTableWrapper"
        );

    if (wrapper) {

        if (stocks.length > 0) {

            wrapper.classList.remove("empty");

        } else {

            wrapper.classList.add("empty");
        }
    }
}


/* =========================================================
   EMA TABLE
========================================================= */

function updateEmaTable() {

    const tbody =
        document.getElementById(
            "emaStockTableBody"
        );

    if (!tbody) {
        return;
    }


    tbody.innerHTML = "";


    const stocks =
        Object.values(liveStocks);


    const totalStocks =
        document.getElementById(
            "totalStocks"
        );


    if (totalStocks) {

        totalStocks.textContent =
            stocks.length;
    }


    stocks.forEach(function (stock, index) {

        const row =
            document.createElement("tr");


        const change =
            number(stock.change);


        const changePercent =
            number(stock.changePercent);


        const ltp =
            number(stock.ltp);


        const open =
            number(stock.open);


        const high =
            number(stock.high);


        const low =
            number(stock.low);


        const previousClose =
            number(stock.previousClose);


        const volume =
            number(stock.volume);


        const totalBid =
            stock.totByQ != null
                ? stock.totByQ
                : "N/A";


        const totalAsk =
            stock.totSlQ != null
                ? stock.totSlQ
                : "N/A";


        row.innerHTML = `

            <td>${index + 1}</td>

            <td>
                <strong>
                    ${getSymbol(stock)}
                </strong>
            </td>

            <td>
                ${formatNumber(ltp)}
            </td>

            <td class="${change >= 0 ? "positive" : "negative"}">
                ${formatNumber(change)}
            </td>

            <td class="${changePercent >= 0 ? "positive" : "negative"}">
                ${formatNumber(changePercent)}%
            </td>

            <td>
                ${formatNumber(open)}
            </td>

            <td>
                ${formatNumber(high)}
            </td>

            <td>
                ${formatNumber(low)}
            </td>

            <td>
                ${formatNumber(previousClose)}
            </td>

            <td>
                ${formatNumber(volume)}
            </td>

            <td>
                ${formatNumber(totalBid)}
            </td>

            <td>
                ${formatNumber(totalAsk)}
            </td>

        `;


        tbody.appendChild(row);

    });


    const wrapper =
        document.getElementById(
            "emaTableWrapper"
        );


    if (wrapper) {

        if (stocks.length > 0) {

            wrapper.classList.remove("empty");

        } else {

            wrapper.classList.add("empty");
        }
    }
}


/* =========================================================
   GET STOCK SYMBOL
========================================================= */

function getSymbol(stock) {

    if (stock.symbol) {

        return stock.symbol;
    }


    if (stock.instrumentKey) {

        return stock.instrumentKey
            .split("|")
            .pop();
    }


    return "UNKNOWN";
}


/* =========================================================
   GET BEST BID
========================================================= */

function getBestBid(stock) {

    if (
        !stock.bidLevels ||
        stock.bidLevels.length === 0
    ) {

        return null;
    }


    const bid =
        stock.bidLevels[0];


    if (!bid) {
        return null;
    }


    return bid.price != null
        ? Number(bid.price)
        : null;
}


/* =========================================================
   GET BEST ASK
========================================================= */

function getBestAsk(stock) {

    if (
        !stock.askLevels ||
        stock.askLevels.length === 0
    ) {

        return null;
    }


    const ask =
        stock.askLevels[0];


    if (!ask) {
        return null;
    }


    return ask.price != null
        ? Number(ask.price)
        : null;
}


/* =========================================================
   NUMBER
========================================================= */

function number(value) {

    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {

        return 0;
    }


    const n =
        Number(value);


    return Number.isFinite(n)
        ? n
        : 0;
}


/* =========================================================
   FORMAT NUMBER
========================================================= */

function formatNumber(value) {

    if (
        value === null ||
        value === undefined ||
        value === "N/A"
    ) {

        return "N/A";
    }


    const n =
        Number(value);


    if (!Number.isFinite(n)) {

        return "N/A";
    }


    return n.toLocaleString(
        "en-IN",
        {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }
    );
}


/* =========================================================
   FORMAT VOLUME
========================================================= */

function formatVolumeCr(volume) {

    const n =
        Number(volume);


    if (!Number.isFinite(n)) {

        return "0";
    }


    return (
        n / 10000000
    ).toFixed(2);
}


/* =========================================================
   START
========================================================= */

connectHistoricalLiveWebSocket();