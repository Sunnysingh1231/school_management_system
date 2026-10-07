
let algoSocket = null;

const liveStocks = new Map();


// =========================================================
// CONNECT ALGO WEBSOCKET
// =========================================================

function connectAlgoWebSocket() {

    const protocol =
        window.location.protocol === "https:"
            ? "wss://"
            : "ws://";

    const wsUrl =
        protocol +
        window.location.host +
        "/ws/algo";

    console.log(
        "Connecting ALGO:",
        wsUrl
    );

    algoSocket =
        new WebSocket(wsUrl);


    // =====================================================
    // OPEN
    // =====================================================

    algoSocket.onopen = function () {

        console.log(
            "✅ ALGO WEBSOCKET CONNECTED"
        );

        setAlgoConnectionStatus(true);
    };


    // =====================================================
    // MESSAGE
    // =====================================================

    algoSocket.onmessage = function (event) {

        try {

            const data =
                JSON.parse(event.data);


            if (!data.instrumentKey) {

                return;
            }


            console.log(
                "ALGO UPDATE:",
                data
            );


            // =============================================
            // STORE LIVE ALGO STOCK
            // =============================================

            liveStocks.set(
                data.instrumentKey,
                data
            );


            // =============================================
            // UPDATE MAIN ALGO STOCK UI
            // =============================================

            processAlgoStock(data);


            // =============================================
            // UPDATE 7 FILTER STAGES
            // =============================================

            updateAlgoFilterBoxes(data);

        } catch (error) {

            console.error(
                "❌ ALGO WEBSOCKET DATA ERROR:",
                error
            );
        }
    };


    // =====================================================
    // CLOSE
    // =====================================================

    algoSocket.onclose = function () {

        console.log(
            "❌ ALGO WEBSOCKET DISCONNECTED"
        );

        setAlgoConnectionStatus(false);


        setTimeout(
            connectAlgoWebSocket,
            3000
        );
    };


    // =====================================================
    // ERROR
    // =====================================================

    algoSocket.onerror = function (error) {

        console.error(
            "❌ ALGO WEBSOCKET ERROR:",
            error
        );
    };
}



// =========================================================
// CONNECTION STATUS
// =========================================================

function setAlgoConnectionStatus(
    connected
) {

    const status =
        document.getElementById(
            "algoConnectionStatus"
        );


    if (!status) {

        return;
    }


    if (connected) {

        status.textContent =
            "LIVE";

        status.className =
            "connection-online";

    } else {

        status.textContent =
            "OFFLINE";

        status.className =
            "connection-offline";
    }
}



// =========================================================
// PROCESS ALGO STOCK
// =========================================================

function processAlgoStock(stock) {

    if (!stock) {

        return;
    }


    updateLiveStockUI(stock);
}



// =========================================================
// LIVE STOCK UI
// =========================================================

function updateLiveStockUI(stock) {

    const row =
        document.getElementById(
            "stock-" +
            makeSafeId(
                stock.instrumentKey
            )
        );


    if (!row) {

        return;
    }


    const ltp =
        row.querySelector(
            ".ltp"
        );


    const change =
        row.querySelector(
            ".change"
        );


    const volume =
        row.querySelector(
            ".volume"
        );


    if (ltp) {

        ltp.textContent =
            formatPrice(
                stock.ltp
            );
    }


    if (change) {

        change.textContent =
            formatPercent(
                stock.changePercent
            );


        change.classList.remove(
            "positive",
            "negative"
        );


        if (
            stock.changePercent > 0
        ) {

            change.classList.add(
                "positive"
            );

        } else if (
            stock.changePercent < 0
        ) {

            change.classList.add(
                "negative"
            );
        }
    }


    if (volume) {

        volume.textContent =
            formatNumber(
                stock.volume
            );
    }
}



// =========================================================
// LOAD ALGO STOCKS
// =========================================================

function loadAlgoStocks() {

    console.log(
        "🔄 Loading algo stocks..."
    );


    const container =
        document.getElementById(
            "algoStocks"
        );


    if (!container) {

        console.warn(
            "⚠️ algoStocks container not found"
        );

        return;
    }


    container.innerHTML = "";


    if (liveStocks.size === 0) {

        container.innerHTML = `
            <div class="empty-message">
                Waiting for live market data...
            </div>
        `;

        return;
    }


    liveStocks.forEach(
        function (stock) {

            if (
                !stock ||
                !stock.instrumentKey
            ) {

                return;
            }


            const safeId =
                makeSafeId(
                    stock.instrumentKey
                );


            const row =
                document.createElement(
                    "div"
                );


            row.id =
                "stock-" +
                safeId;


            row.className =
                "algo-stock-row";


            row.innerHTML = `
                <div class="stock-symbol">
                    ${stock.symbol || "-"}
                </div>

                <div class="ltp">
                    ${formatPrice(stock.ltp)}
                </div>

                <div class="change">
                    ${formatPercent(stock.changePercent)}
                </div>

                <div class="volume">
                    ${formatNumber(stock.volume)}
                </div>
            `;


            container.appendChild(
                row
            );
        }
    );


    console.log(
        "✅ Algo stocks loaded:",
        liveStocks.size
    );
}



// =========================================================
// UPDATE 7 ALGO FILTER BOXES
// =========================================================

function updateAlgoFilterBoxes(
    stock
) {

    if (!stock) {

        return;
    }


    // =====================================================
    // STEP 1
    // =====================================================

    updateStageBox(
        "step1Stocks",
        stock,
        true
    );


    // =====================================================
    // STEP 2
    // =====================================================

    updateStageBox(
        "step2Stocks",
        stock,
        stock.liquidityPassed
    );


    // =====================================================
    // STEP 3
    // =====================================================

    updateStageBox(
        "step3Stocks",
        stock,
        stock.liquidityPassed &&
        stock.trendPassed
    );


    // =====================================================
    // STEP 4
    // =====================================================

    updateStageBox(
        "step4Stocks",
        stock,
        stock.liquidityPassed &&
        stock.trendPassed &&
        stock.momentumPassed
    );


    // =====================================================
    // STEP 5
    // =====================================================

    updateStageBox(
        "step5Stocks",
        stock,
        stock.liquidityPassed &&
        stock.trendPassed &&
        stock.momentumPassed &&
        stock.volumeVwapPassed
    );


    // =====================================================
    // STEP 6
    // =====================================================

    updateStageBox(
        "step6Stocks",
        stock,
        stock.liquidityPassed &&
        stock.trendPassed &&
        stock.momentumPassed &&
        stock.volumeVwapPassed &&
        stock.atrSetupPassed
    );


    // =====================================================
    // STEP 7
    // =====================================================

    updateStageBox(
        "step7Stocks",
        stock,
        stock.liquidityPassed &&
        stock.trendPassed &&
        stock.momentumPassed &&
        stock.volumeVwapPassed &&
        stock.atrSetupPassed &&
        stock.entryRiskPassed
    );


    // =====================================================
    // FINAL TRADE DETAILS
    // =====================================================

    updateFinalTradeBox(stock);
}



// =========================================================
// STAGE BOX
// =========================================================

function updateStageBox(
    containerId,
    stock,
    passed
) {

    const container =
        document.getElementById(
            containerId
        );


    if (!container) {

        return;
    }


    const safeId =
        makeSafeId(
            stock.instrumentKey
        );


    let row =
        document.getElementById(
            containerId +
            "-" +
            safeId
        );


    // =====================================================
    // STOCK PASSED
    // =====================================================

    if (passed) {

        if (!row) {

            row =
                document.createElement(
                    "div"
                );


            row.id =
                containerId +
                "-" +
                safeId;


            row.className =
                "algo-filter-stock";


            container.appendChild(
                row
            );
        }


        row.innerHTML = `
            <div class="stock-symbol">
                ${stock.symbol || "-"}
            </div>

            <div class="stock-ltp">
                ₹${formatPrice(stock.ltp)}
            </div>

            <div class="stock-regime">
                ${stock.marketRegime || "-"}
            </div>
        `;


        return;
    }


    // =====================================================
    // STOCK FAILED
    // =====================================================

    if (row) {

        row.remove();
    }
}



// =========================================================
// STEP 7 DETAILS
// =========================================================

function updateFinalTradeBox(
    stock
) {

    const container =
        document.getElementById(
            "step7Stocks"
        );


    if (!container) {

        return;
    }


    if (!stock.entryRiskPassed) {

        return;
    }


    const safeId =
        makeSafeId(
            stock.instrumentKey
        );


    const row =
        document.getElementById(
            "step7Stocks-" +
            safeId
        );


    if (!row) {

        return;
    }


    row.innerHTML = `
        <div class="stock-symbol">
            ${stock.symbol || "-"}
        </div>

        <div>
            ₹${formatPrice(stock.ltp)}
        </div>

        <div>
            ${stock.signal || "-"}
        </div>

        <div>
            Entry:
            ₹${formatPrice(stock.entry)}
        </div>

        <div>
            SL:
            ₹${formatPrice(stock.stopLoss)}
        </div>

        <div>
            Target:
            ₹${formatPrice(stock.target)}
        </div>

        <div>
            R:R:
            ${formatPrice(stock.riskReward)}
        </div>
    `;
}



// =========================================================
// SAFE ID
// =========================================================

function makeSafeId(
    value
) {

    return String(value)
        .replaceAll("|", "-")
        .replaceAll(" ", "-")
        .replaceAll("/", "-");
}



// =========================================================
// PRICE
// =========================================================

function formatPrice(
    value
) {

    const number =
        Number(value || 0);

    return number.toFixed(2);
}



// =========================================================
// PERCENT
// =========================================================

function formatPercent(
    value
) {

    const number =
        Number(value || 0);

    return number.toFixed(2) + "%";
}



// =========================================================
// NUMBER
// =========================================================

function formatNumber(
    value
) {

    return Number(value || 0)
        .toLocaleString("en-IN");
}



// =========================================================
// GET LATEST STOCK
// =========================================================

function getLatestStock(
    instrumentKey
) {

    return liveStocks.get(
        instrumentKey
    );
}



// =========================================================
// DOM READY
// =========================================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        console.log(
            "========== ALGO DOM READY =========="
        );

        connectAlgoWebSocket();

        loadAlgoStocks();
    }
);