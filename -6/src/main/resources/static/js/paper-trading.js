
let socket;
let priceChart;

const marketData = {};

/* =====================================================
   PAGINATION
===================================================== */

const STOCKS_PER_PAGE = 20;
let currentPage = 1;
let searchText = "";
let selectedInstrumentKey = null;
let tableInitialized = false;

/* =====================================================
   DEPTH SYMBOLS
===================================================== */

const depthSymbol = new Set();

/* =====================================================
   TOP GAINERS DATA
===================================================== */

const topGainersData = new Map();

/* =====================================================
   CONNECT TOP GAINERS WEBSOCKET
===================================================== */

function connectMarket() {
    const protocol =
        window.location.protocol === "https:" ? "wss" : "ws";

    const url =
        protocol + "://" + window.location.host + "/ws/top-gainers";

    console.log("Connecting TOP GAINERS:", url);

    // Duplicate connection prevent karo
    if (
        socket &&
        (socket.readyState === WebSocket.OPEN ||
         socket.readyState === WebSocket.CONNECTING)
    ) {
        console.log("TOP GAINERS SOCKET ALREADY CONNECTED");
        return;
    }

    socket = new WebSocket(url);

    socket.onopen = function () {
        console.log("TOP GAINERS SOCKET CONNECTED");
        setConnection(true, "Live");
    };

    socket.onmessage = function (event) {
        try {
            const data = JSON.parse(event.data);

            console.log("TOP GAINER DATA:", data);

            // Server ka time message stock nahi hai
            if (data && data.type === "time") {
                console.log("SERVER TIME:", data.time);
                return;
            }

            // Java se complete array aane par
            if (Array.isArray(data)) {
                topGainersData.clear();

                data.forEach(function (stock) {
                    if (
                        stock &&
                        stock.instrumentKey
                    ) {
                        topGainersData.set(
                            stock.instrumentKey,
                            stock
                        );
                    }
                });
            }

            // Single stock object aane par
            else if (
                data &&
                data.instrumentKey
            ) {
                topGainersData.set(
                    data.instrumentKey,
                    data
                );
            }

            else {
                console.warn(
                    "Unexpected WebSocket message:",
                    data
                );
                return;
            }

            console.log(
                "STOCKS RECEIVED:",
                topGainersData.size
            );

            // UI update
            renderTopGainers();

            // Last update time
            const lastUpdate2 =
                document.getElementById("lastUpdate2");

            if (lastUpdate2) {
                lastUpdate2.textContent =
                    new Date().toLocaleTimeString("en-IN", {
                        hour: "2-digit",
                        minute: "2-digit",
                        second: "2-digit",
                        hour12: false
                    });
            }

        } catch (error) {
            console.error(
                "TOP GAINERS JSON ERROR:",
                error,
                event.data
            );
        }
    };

    socket.onclose = function (event) {
        console.log(
            "TOP GAINERS SOCKET CLOSED:",
            event.code,
            event.reason
        );

        setConnection(false, "Disconnected");

        // Reconnect only if this is still the active socket
        setTimeout(function () {
            if (socket && socket.readyState === WebSocket.CLOSED) {
                connectMarket();
            }
        }, 3000);
    };

    socket.onerror = function (error) {
        console.error("TOP GAINERS WEBSOCKET ERROR:", error);
    };
}

/* =====================================================
   RENDER TOP 10 GAINERS
===================================================== */

function renderTopGainers() {
    const container =
        document.getElementById("topMoversContainer");

    const empty =
        document.getElementById("moversEmpty");

    if (!container) {
        console.warn("topMoversContainer not found");
        return;
    }

    const stocks = Array.from(topGainersData.values());

    const gainers = stocks
        .filter(function (stock) {
            return Number(stock.changePercent) > 0;
        })
        .sort(function (a, b) {
            return Number(b.changePercent) -
                   Number(a.changePercent);
        })
        .slice(0, 10);

    if (gainers.length === 0) {
        container.innerHTML = "";

        if (empty) {
            empty.style.display = "block";
            empty.textContent = stocks.length
                ? "No positive gainers available."
                : "Waiting for live market data...";
        }

        return;
    }

    if (empty) {
        empty.style.display = "none";
    }

    container.innerHTML = gainers.map(function (stock, index) {
        const ltp = Number(stock.ltp || 0);
        const change = Number(stock.change || 0);
        const changePercent = Number(stock.changePercent || 0);

        const symbol =
            stock.symbol ||
            (stock.instrumentKey || "").split("|").pop() ||
            "N/A";

        const logo = symbol.substring(0, 2).toUpperCase();

        const sign = change >= 0 ? "+" : "";
        const percentSign = changePercent >= 0 ? "+" : "";

        return `
            <div class="mover-row">
                <div>
                    <div class="mover-rank">${index + 1}</div>
                </div>

                <div class="mover-stock">
                    <div class="stock-logo">${logo}</div>
                    <div>
                        <div class="mover-symbol">${symbol}</div>
                        <div class="mover-name">NSE Equity</div>
                    </div>
                </div>

                <div class="mover-price">
                    ₹${ltp.toLocaleString("en-IN", {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2
                    })}
                </div>

                <div class="mover-change gainer">
                    ${change >= 0 ? "▲" : "▼"}
                    ${sign}${change.toFixed(2)}
                </div>

                <div>
                    <span class="mover-percent gainer-bg">
                        ${percentSign}${changePercent.toFixed(2)}%
                    </span>
                </div>
            </div>
        `;
    }).join("");
}

/* =====================================================
   CONNECTION STATUS
===================================================== */

function setConnection(online, text) {
    const dot = document.getElementById("connectionDot");
    const status = document.getElementById("connectionText");

    if (dot) {
        dot.classList.toggle("online", online);
        dot.classList.toggle("offline", !online);
    }

    if (status) {
        status.textContent = text;
    }
}

/* =====================================================
   START
===================================================== */

connectMarket();