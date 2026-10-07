
// =====================================================
// EMA
// =====================================================

let emaStocks = new Map();

function updateEMATable() {

    const tbody =
        document.getElementById("emaStockTableBody");

    const wrapper =
        document.getElementById("emaTableWrapper");

    tbody.innerHTML = "";

    if (emaStocks.size === 0) {

        wrapper.classList.add("empty");
        wrapper.classList.remove("scrollable");

        document.getElementById("totalStocks").textContent = "0";

        return;
    }

    wrapper.classList.remove("empty");

    if (emaStocks.size > 10) {
        wrapper.classList.add("scrollable");
    } else {
        wrapper.classList.remove("scrollable");
    }

    let index = 1;

    emaStocks.forEach(stock => {

        const row =
            document.createElement("tr");

        // =============================================
        // CHANGE
        // =============================================

        const change =
            Number(stock.change) || 0;

        // =============================================
        // CHANGE %
        // =============================================

        const changePercent =
            Number(stock.changePercent) || 0;

        // =============================================
        // CHANGE COLOR
        // =============================================

        const changeClass =
            change > 0
                ? "bullish"
                : change < 0
                    ? "bearish"
                    : "neutral";

        // =============================================
        // CHANGE % COLOR
        // =============================================

        const changePercentClass =
            changePercent > 0
                ? "bullish"
                : changePercent < 0
                    ? "bearish"
                    : "neutral";

        // =============================================
        // ROW
        // =============================================

        row.innerHTML = `

            <td>
                ${index}
            </td>

            <td>
                <span class="stock-name">
                    ${stock.symbol || "-"}
                </span>
            </td>

            <td>
                <span class="ltp">
                    ₹${formatNumber(stock.ltp)}
                </span>
            </td>

            <!-- CHANGE -->

            <td class="${changeClass}">
                ${change > 0 ? "+" : ""}
                ${formatNumber(change)}
            </td>

            <!-- CHANGE % -->

            <td class="${changePercentClass}">
                ${changePercent > 0 ? "+" : ""}
                ${formatNumber(changePercent)}%
            </td>

            <td>
                ₹${formatNumber(stock.open)}
            </td>

            <td>
                ₹${formatNumber(stock.high)}
            </td>

            <td>
                ₹${formatNumber(stock.low)}
            </td>

            <td>
                ₹${formatNumber(stock.previousClose)}
            </td>

            <td>
                ${formatVolume(stock.volume)}
            </td>

            <td>
                ${formatVolume(stock.totByQ)}
            </td>

            <td>
                ${formatVolume(stock.totSlQ)}
            </td>

        `;

        tbody.appendChild(row);

        index++;
    });

    document.getElementById("totalStocks").textContent =
        emaStocks.size;
}


// =====================================================
// LIQUIDITY DATA
// =====================================================

// Latest liquidity websocket data yahan store hoga.
// Search / Sort isi data ko dobara render karega.

let lastLiquidityStocks = [];


// =====================================================
// LIQUIDITY
// =====================================================

function renderLiquidityMetrics(stocks) {

    const tbody =
        document.getElementById(
            "liquidityStockTableBody"
        );

    const wrapper =
        document.getElementById(
            "liquidityTableWrapper"
        );

    const totalStocks =
        document.getElementById(
            "liquidityTotalStocks"
        );


    // -----------------------------------------------
    // HTML ELEMENT CHECK
    // -----------------------------------------------

    if (!tbody || !wrapper || !totalStocks) {

        console.error(
            "Liquidity HTML elements not found"
        );

        return;
    }


    // -----------------------------------------------
    // CLEAR OLD ROWS
    // -----------------------------------------------

    tbody.innerHTML = "";


    // -----------------------------------------------
    // NO DATA
    // -----------------------------------------------

    if (
        !Array.isArray(stocks) ||
        stocks.length === 0
    ) {

        wrapper.classList.add("empty");

        wrapper.classList.remove("scrollable");

        totalStocks.textContent = "0";

        return;
    }


    wrapper.classList.remove("empty");


    // =================================================
    // COPY DATA
    // =================================================

    let filteredStocks = [...stocks];


    // =================================================
    // SEARCH
    // =================================================

    const searchInput =
        document.getElementById(
            "liquiditySearch"
        );

    const searchText =
        searchInput
            ? searchInput.value.trim().toLowerCase()
            : "";


    if (searchText !== "") {

        filteredStocks =
            filteredStocks.filter(stock => {

                const searchableData = [

                    // Stock
                    stock.symbol,
                    stock.instrumentKey,

                    // Price
                    stock.ltp,

                    // Change
                    stock.change,
                    stock.changePercent,

                    // Volume
                    stock.volume,

                    // Traded Value
                    stock.tradedValueCrore,

                    // Bid / Ask
                    stock.bestBid,
                    stock.bestAsk,

                    // Spread
                    stock.spread,
                    stock.spreadPercent,

                    // Safety
                    stock.tradedValueSafe,
                    stock.spreadSafe,
                    stock.liquiditySafe

                ]
                    .map(value =>
                        String(
                            value ?? ""
                        ).toLowerCase()
                    )
                    .join(" ");

                return searchableData.includes(
                    searchText
                );
            });
    }


    // =================================================
    // SORT
    // =================================================

    const sortSelect =
        document.getElementById(
            "liquiditySort"
        );

    const sortType =
        sortSelect
            ? sortSelect.value
            : "default";


    filteredStocks.sort((a, b) => {

        const aVolume =
            Number(a.volume) || 0;

        const bVolume =
            Number(b.volume) || 0;


        const aTradedValue =
            Number(
                a.tradedValueCrore
            ) || 0;

        const bTradedValue =
            Number(
                b.tradedValueCrore
            ) || 0;


        const aLtp =
            Number(a.ltp) || 0;

        const bLtp =
            Number(b.ltp) || 0;


        const aChangePercent =
            Number(
                a.changePercent
            ) || 0;

        const bChangePercent =
            Number(
                b.changePercent
            ) || 0;


        switch (sortType) {

            // =========================================
            // VOLUME HIGH → LOW
            // =========================================

            case "volume":

                return bVolume - aVolume;


            // =========================================
            // TRADED VALUE HIGH → LOW
            // =========================================

            case "tradedValue":

                return (
                    bTradedValue -
                    aTradedValue
                );


            // =========================================
            // LTP HIGH → LOW
            // =========================================

            case "ltp":

                return bLtp - aLtp;


            // =========================================
            // TOP GAINER
            // =========================================

            case "topGainer":

                return (
                    bChangePercent -
                    aChangePercent
                );


            // =========================================
            // TOP LOSER
            // =========================================

            case "topLoser":

                return (
                    aChangePercent -
                    bChangePercent
                );


            // =========================================
            // DEFAULT
            // =========================================

            default:

                return 0;
        }

    });


    // =================================================
    // TABLE SCROLL
    // =================================================

    if (filteredStocks.length > 10) {

        wrapper.classList.add(
            "scrollable"
        );

    } else {

        wrapper.classList.remove(
            "scrollable"
        );
    }


    // =================================================
    // RENDER
    // =================================================

    let index = 1;


    filteredStocks.forEach(stock => {

        const row =
            document.createElement("tr");


        // =============================================
        // SAFETY
        // =============================================

        const tradedValueSafe =
            Boolean(
                stock.tradedValueSafe
            );

        const spreadSafe =
            Boolean(
                stock.spreadSafe
            );

        const liquiditySafe =
            Boolean(
                stock.liquiditySafe
            );


        // =============================================
        // CHANGE %
        // =============================================

        const changePercent =
            Number(stock.changePercent) || 0;


        // =============================================
        // CHANGE % COLOR
        // =============================================

        const changePercentClass =
            changePercent > 0
                ? "bullish"
                : changePercent < 0
                    ? "bearish"
                    : "neutral";


        // =============================================
        // SAFETY CLASSES
        // =============================================

        const tradedValueClass =
            tradedValueSafe
                ? "bullish"
                : "bearish";


        const spreadClass =
            spreadSafe
                ? "bullish"
                : "bearish";


        const liquidityClass =
            liquiditySafe
                ? "bullish"
                : "bearish";


        // =============================================
        // ROW
        // =============================================

        row.innerHTML = `

            <td>
                ${index}
            </td>


            <td>
                <span class="stock-name">
                    ${stock.symbol || "-"}
                </span>
            </td>


            <td>
                <span class="ltp">
                    ₹${formatNumber(stock.ltp)}
                </span>
            </td>


            <!-- CHANGE % -->

            <td class="${changePercentClass}">
                ${changePercent > 0 ? "+" : ""}
                ${formatNumber(changePercent)}%
            </td>


            <!-- VOLUME -->

            <td>
                ${formatVolume(stock.volume)}
            </td>


            <!-- TRADED VALUE -->

            <td>
                ${formatNumber(
            stock.tradedValueCrore
        )} Cr
            </td>


            <!-- BEST BID -->

            <td>
                ₹${formatNumber(
            stock.bestBid
        )}
            </td>


            <!-- BEST ASK -->

            <td>
                ₹${formatNumber(
            stock.bestAsk
        )}
            </td>


            <!-- SPREAD -->

            <td>
                ₹${formatNumber(
            stock.spread
        )}
            </td>


            <!-- SPREAD % -->

            <td>
                ${formatNumber(
            stock.spreadPercent
        )}%
            </td>


            <!-- TRADED VALUE SAFE -->

            <td class="${tradedValueClass}">
                ${tradedValueSafe
                ? "YES"
                : "NO"
            }
            </td>


            <!-- SPREAD SAFE -->

            <td class="${spreadClass}">
                ${spreadSafe
                ? "YES"
                : "NO"
            }
            </td>


            <!-- LIQUIDITY SAFE -->

            <td class="${liquidityClass}">
                ${liquiditySafe
                ? "✅"
                : "❌"
            }
            </td>

        `;


        tbody.appendChild(row);

        index++;
    });


    // =================================================
    // DISPLAY COUNT
    // =================================================

    totalStocks.textContent =
        filteredStocks.length;
}


// =====================================================
// FORMAT
// =====================================================

function formatNumber(value) {

    if (
        value === null ||
        value === undefined ||
        isNaN(value)
    ) {

        return "-";
    }

    return Number(value).toFixed(2);
}


function formatVolume(value) {

    if (
        value === null ||
        value === undefined ||
        isNaN(value)
    ) {

        return "-";
    }

    return Number(value).toLocaleString(
        "en-IN"
    );
}


// =====================================================
// EMA WEBSOCKET
// =====================================================

let emaSocket = null;


function connectEMAWebSocket() {

    if (
        emaSocket &&
        (
            emaSocket.readyState ===
            WebSocket.OPEN ||
            emaSocket.readyState ===
            WebSocket.CONNECTING
        )
    ) {

        return;
    }


    const protocol =
        window.location.protocol ===
            "https:"
            ? "wss:"
            : "ws:";


    const url =
        protocol +
        "//" +
        window.location.host +
        "/ws/market";


    emaSocket =
        new WebSocket(url);


    // =============================================
    // OPEN
    // =============================================

    emaSocket.onopen =
        function() {

            console.log(
                "EMA WEBSOCKET CONNECTED"
            );


            const dot =
                document.getElementById(
                    "connectionDot"
                );


            if (dot) {

                dot.classList.remove(
                    "disconnected"
                );

                dot.classList.add(
                    "connected"
                );
            }


            const connectionText =
                document.getElementById(
                    "connectionText"
                );


            if (connectionText) {

                connectionText.textContent =
                    "Connected";
            }

        };


    // =============================================
    // MESSAGE
    // =============================================

    emaSocket.onmessage =
        function(event) {

            try {

                const data =
                    JSON.parse(
                        event.data
                    );


                if (
                    data.symbol &&
                    data.ltp !== undefined
                ) {

                    emaStocks.set(
                        data.symbol,
                        data
                    );

                    // Agar EMA table live update
                    // karna ho to uncomment karo:
                    //
                    // updateEMATable();
                }

            } catch (error) {

                console.error(
                    "EMA JSON ERROR:",
                    error
                );
            }

        };


    // =============================================
    // CLOSE
    // =============================================

    emaSocket.onclose =
        function() {

            const dot =
                document.getElementById(
                    "connectionDot"
                );


            if (dot) {

                dot.classList.remove(
                    "connected"
                );

                dot.classList.add(
                    "disconnected"
                );
            }


            const connectionText =
                document.getElementById(
                    "connectionText"
                );


            if (connectionText) {

                connectionText.textContent =
                    "Disconnected";
            }


            setTimeout(
                connectEMAWebSocket,
                3000
            );

        };


    // =============================================
    // ERROR
    // =============================================

    emaSocket.onerror =
        function(error) {

            console.error(
                "EMA WEBSOCKET ERROR:",
                error
            );

        };
}


// =====================================================
// LIQUIDITY WEBSOCKET
// =====================================================

let liquiditySocket = null;


function connectLiquiditySocket() {

    if (
        liquiditySocket &&
        (
            liquiditySocket.readyState ===
            WebSocket.OPEN ||
            liquiditySocket.readyState ===
            WebSocket.CONNECTING
        )
    ) {

        return;
    }


    const protocol =
        window.location.protocol ===
            "https:"
            ? "wss:"
            : "ws:";


    const url =
        protocol +
        "//" +
        window.location.host +
        "/ws/liquidity-metrics";


    liquiditySocket =
        new WebSocket(url);


    // =============================================
    // OPEN
    // =============================================

    liquiditySocket.onopen =
        function() {

            console.log(
                "LIQUIDITY WEBSOCKET CONNECTED"
            );

        };


    // =============================================
    // MESSAGE
    // =============================================

    liquiditySocket.onmessage =
        function(event) {

            try {

                const stocks =
                    JSON.parse(
                        event.data
                    );


                console.log(
                    "LIQUIDITY DATA:",
                    stocks
                );


                // Latest data save karo

                if (Array.isArray(stocks)) {

                    lastLiquidityStocks =
                        stocks;

                } else {

                    console.error(
                        "Liquidity data is not an array:",
                        stocks
                    );

                    return;
                }


                // Initial render

                renderLiquidityMetrics(
                    lastLiquidityStocks
                );

            } catch (error) {

                console.error(
                    "LIQUIDITY JSON ERROR:",
                    error
                );

            }

        };


    // =============================================
    // ERROR
    // =============================================

    liquiditySocket.onerror =
        function(error) {

            console.error(
                "LIQUIDITY WEBSOCKET ERROR:",
                error
            );

        };


    // =============================================
    // CLOSE
    // =============================================

    liquiditySocket.onclose =
        function() {

            console.log(
                "LIQUIDITY WEBSOCKET CLOSED"
            );


            liquiditySocket = null;


            setTimeout(
                connectLiquiditySocket,
                3000
            );

        };
}


// =====================================================
// SEARCH + SORT EVENTS
// =====================================================

function setupLiquidityControls() {

    // =============================================
    // SEARCH
    // =============================================

    const searchInput =
        document.getElementById(
            "liquiditySearch"
        );


    if (searchInput) {

        searchInput.addEventListener(
            "input",
            function() {

                renderLiquidityMetrics(
                    lastLiquidityStocks
                );

            }
        );

    } else {

        console.warn(
            "liquiditySearch element not found"
        );
    }


    // =============================================
    // SORT
    // =============================================

    const sortSelect =
        document.getElementById(
            "liquiditySort"
        );


    if (sortSelect) {

        sortSelect.addEventListener(
            "change",
            function() {

                renderLiquidityMetrics(
                    lastLiquidityStocks
                );

            }
        );

    } else {

        console.warn(
            "liquiditySort element not found"
        );
    }
}


// =====================================================
// PAGE LOAD
// =====================================================

document.addEventListener(
    "DOMContentLoaded",
    function() {

        console.log(
            "PAGE INITIALIZED"
        );


        // WebSockets

        connectEMAWebSocket();

        connectLiquiditySocket();


        // Search + Sort

        setupLiquidityControls();

    }
);
