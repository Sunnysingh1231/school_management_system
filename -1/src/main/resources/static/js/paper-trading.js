let socket = null;

const liveStocks = {};

let selectedStocks = [];

let positions = [];


// ========================================
// PAGE LOAD
// ========================================

document.addEventListener("DOMContentLoaded", () => {

    loadStocks();

    loadPositions();

    loadHistory();

    connectMarketWebSocket();

});


// ========================================
// LOAD RANDOM 10
// ========================================

async function loadStocks() {

    try {

        const response =
            await fetch("/paper-trading/stocks");

        if (!response.ok) {
            throw new Error(
                "Unable to load stocks"
            );
        }

        const stocks =
            await response.json();

        selectedStocks = stocks;

        stocks.forEach(stock => {

            liveStocks[
                stock.instrumentKey
            ] = stock;

        });

        renderStocks();

    } catch (error) {

        console.error(error);

        document.getElementById(
            "stocksContainer"
        ).innerHTML = `
            <div class="empty">
                Live stocks are not available yet.
                Please try again.
            </div>
        `;
    }
}


// ========================================
// WEBSOCKET
// ========================================

function connectMarketWebSocket() {

    const protocol =
        window.location.protocol === "https:"
            ? "wss:"
            : "ws:";

    const wsUrl =
        protocol +
        "//" +
        window.location.host +
        "/ws/market";

    socket = new WebSocket(wsUrl);


    socket.onopen = () => {

        document.getElementById(
            "connectionDot"
        ).className = "dot online";

        document.getElementById(
            "connectionText"
        ).innerText = "Live";

    };


    socket.onmessage = event => {

        try {

            const data =
                JSON.parse(event.data);

            //processLiveUpdate(data);
			console.log(data)

        } catch (error) {

            console.error(
                "WebSocket JSON error:",
                error
            );

        }

    };


    socket.onclose = () => {

        document.getElementById(
            "connectionDot"
        ).className = "dot";

        document.getElementById(
            "connectionText"
        ).innerText =
            "Disconnected";

        setTimeout(
            connectMarketWebSocket,
            3000
        );

    };


    socket.onerror = error => {

        console.error(
            "WebSocket error:",
            error
        );

    };

}


// ========================================
// LIVE UPDATE
// ========================================

function processLiveUpdate(data) {

    if (!data.instrumentKey) {
        return;
    }


    // Store latest data
    liveStocks[
        data.instrumentKey
    ] = data;


    // Random 10 card
    if (
        selectedStocks.some(
            stock =>
                stock.instrumentKey ===
                data.instrumentKey
        )
    ) {

        updateStockCard(data);
		console.log(data)

    }


    // Current position P/L
    updatePositionLiveData(data);

}


// ========================================
// RENDER RANDOM STOCKS
// ========================================

function renderStocks() {

    const container =
        document.getElementById(
            "stocksContainer"
        );

    if (!selectedStocks.length) {

        container.innerHTML = `
            <div class="empty">
                No live stocks available.
            </div>
        `;

        return;
    }


    container.innerHTML =
        selectedStocks
            .map(createStockCard)
            .join("");

}


// ========================================
// STOCK CARD
// ========================================

function createStockCard(stock) {

    const positive =
        stock.change >= 0;

    const changeClass =
        positive
            ? "positive"
            : "negative";

    const sign =
        positive ? "+" : "";

    const id =
        safeId(stock.instrumentKey);

    return `

        <div
            class="stock-card"
            id="stock-${id}"
        >

            <div class="stock-head">

                <div class="symbol">
                    ${escapeHtml(stock.symbol)}
                </div>

                <div class="live-badge">
                    LIVE
                </div>

            </div>


            <div class="price"
                 id="price-${id}">

                ₹${formatNumber(stock.ltp)}

            </div>


            <div
                class="change ${changeClass}"
                id="change-${id}"
            >

                ${sign}${formatNumber(stock.change)}
                (${sign}${formatNumber(stock.changePercent)}%)

            </div>


            <div class="details">

                <div class="detail">

                    <span>Open</span>

                    <strong id="open-${id}">
                        ₹${formatNumber(stock.open)}
                    </strong>

                </div>


                <div class="detail">

                    <span>High</span>

                    <strong id="high-${id}">
                        ₹${formatNumber(stock.high)}
                    </strong>

                </div>


                <div class="detail">

                    <span>Low</span>

                    <strong id="low-${id}">
                        ₹${formatNumber(stock.low)}
                    </strong>

                </div>


                <div class="detail">

                    <span>Volume</span>

                    <strong id="volume-${id}">
                        ${formatNumber(stock.volume)}
                    </strong>

                </div>

            </div>


            <input
                type="number"
                min="1"
                value="1"
                class="quantity"
                id="qty-${id}"
                placeholder="Quantity"
            >


            <div class="action-row">

                <button
                    class="buy-btn"
                    onclick="buyStock(
                        '${escapeJs(stock.instrumentKey)}'
                    )"
                >
                    BUY
                </button>


                <button
                    class="sell-btn"
                    onclick="sellStock(
                        '${escapeJs(stock.instrumentKey)}'
                    )"
                >
                    SELL
                </button>

            </div>

        </div>
    `;
}


// ========================================
// UPDATE STOCK CARD
// ========================================

function updateStockCard(stock) {

    const id =
        safeId(stock.instrumentKey);


    const price =
        document.getElementById(
            `price-${id}`
        );

    if (!price) {
        return;
    }


    price.innerText =
        "₹" +
        formatNumber(stock.ltp);


    const change =
        document.getElementById(
            `change-${id}`
        );


    const positive =
        stock.change >= 0;


    change.className =
        "change " +
        (
            positive
                ? "positive"
                : "negative"
        );


    change.innerText =
        `${positive ? "+" : ""}${formatNumber(stock.change)}
        (${positive ? "+" : ""}${formatNumber(stock.changePercent)}%)`;


    document.getElementById(
        `open-${id}`
    ).innerText =
        "₹" +
        formatNumber(stock.open);


    document.getElementById(
        `high-${id}`
    ).innerText =
        "₹" +
        formatNumber(stock.high);


    document.getElementById(
        `low-${id}`
    ).innerText =
        "₹" +
        formatNumber(stock.low);


    document.getElementById(
        `volume-${id}`
    ).innerText =
        formatNumber(stock.volume);

}


// ========================================
// BUY
// ========================================

async function buyStock(
    instrumentKey
) {

    const id =
        safeId(instrumentKey);

    const quantityInput =
        document.getElementById(
            `qty-${id}`
        );


    const quantity =
        parseInt(
            quantityInput.value
        );


    if (
        !quantity ||
        quantity <= 0
    ) {

        showToast(
            "Quantity must be greater than zero"
        );

        return;
    }


    const latest =
        liveStocks[instrumentKey];


    if (
        !latest ||
        latest.ltp <= 0
    ) {

        showToast(
            "Live price not available"
        );

        return;
    }


    try {

        const response =
            await fetch(
                "/paper-trading/buy",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({
                        instrumentKey:
                            instrumentKey,

                        quantity:
                            quantity
                    })
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            throw new Error(
                data.error ||
                "BUY failed"
            );

        }


        showToast(
            `${data.symbol} BUY executed at ₹${formatNumber(data.buyPrice)}`
        );


        await loadPositions();

    } catch (error) {

        showToast(
            error.message
        );

    }

}


// ========================================
// SELL
// ========================================

async function sellStock(
    instrumentKey
) {

    try {

        const response =
            await fetch(
                `/paper-trading/sell?instrumentKey=${encodeURIComponent(instrumentKey)}`,
                {
                    method: "POST"
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            throw new Error(
                data.error ||
                "SELL failed"
            );

        }


        const pnl =
            data.profitLoss;


        showToast(
            `${data.symbol} SELL executed | P/L ₹${formatNumber(pnl)}`
        );


        await loadPositions();

        await loadHistory();

    } catch (error) {

        showToast(
            error.message
        );

    }

}


// ========================================
// CURRENT POSITIONS
// ========================================

async function loadPositions() {

    try {

        const response =
            await fetch(
                "/paper-trading/positions"
            );

        positions =
            await response.json();

        renderPositions();

    } catch (error) {

        console.error(
            "Positions error:",
            error
        );

    }

}


// ========================================
// RENDER POSITIONS
// ========================================

function renderPositions() {

    const container =
        document.getElementById(
            "positionsContainer"
        );


    if (!positions.length) {

        container.innerHTML = `
            <div class="empty">
                No paper positions yet.
            </div>
        `;

        return;
    }


    container.innerHTML =
        positions
            .map(createPositionCard)
            .join("");

}


// ========================================
// POSITION CARD
// ========================================

function createPositionCard(position) {

    const live =
        liveStocks[
            position.instrumentKey
        ];


    const currentPrice =
        live
            ? live.ltp
            : (
                position.status === "SOLD"
                    ? position.sellPrice
                    : position.buyPrice
            );


    let pnl = 0;
    let pnlPercent = 0;


    if (
        position.status === "BOUGHT"
    ) {

        pnl =
            (
                currentPrice -
                position.buyPrice
            ) *
            position.quantity;


        pnlPercent =
            position.buyPrice !== 0
                ? (
                    (
                        currentPrice -
                        position.buyPrice
                    )
                    /
                    position.buyPrice
                ) * 100
                : 0;

    } else {

        pnl =
            (
                position.sellPrice -
                position.buyPrice
            ) *
            position.quantity;


        pnlPercent =
            position.buyPrice !== 0
                ? (
                    (
                        position.sellPrice -
                        position.buyPrice
                    )
                    /
                    position.buyPrice
                ) * 100
                : 0;

    }


    const pnlClass =
        pnl >= 0
            ? "positive"
            : "negative";


    const statusClass =
        position.status === "BOUGHT"
            ? "status-bought"
            : "status-sold";


			const button =
			    position.status === "BOUGHT"

			        ? `
			            <button
			                class="sell-btn"
			                onclick="sellStock(
			                    '${escapeJs(position.instrumentKey)}'
			                )"
			            >
			                SELL
			            </button>
			          `

			        : `
			            <button
			                class="buy-btn"
			                onclick="buyExistingStock(
			                    '${escapeJs(position.instrumentKey)}'
			                )"
			            >
			                BUY
			            </button>
			          `;


    return `

        <div
            class="position-card"
            id="position-${safeId(
                position.instrumentKey
            )}"
        >

            <div class="position-top">

                <div class="position-symbol">
                    ${escapeHtml(position.symbol)}
                </div>

                <div
                    class="status-badge ${statusClass}"
                >
                    ${position.status}
                </div>

            </div>


            <div class="position-grid">

                <div class="position-value">

                    <span>Buy Price</span>

                    ₹${formatNumber(
                        position.buyPrice
                    )}

                </div>


                <div class="position-value">

                    <span>Current Price</span>

                    ₹${formatNumber(
                        currentPrice
                    )}

                </div>


                <div class="position-value">

                    <span>Quantity</span>

                    ${position.quantity}

                </div>


                <div class="position-value">

                    <span>Profit / Loss</span>

                    <strong class="${pnlClass}">
                        ${pnl >= 0 ? "+" : ""}
                        ₹${formatNumber(pnl)}
                    </strong>

                </div>


                <div class="position-value">

                    <span>P/L %</span>

                    <strong class="${pnlClass}">
                        ${pnl >= 0 ? "+" : ""}
                        ${formatNumber(pnlPercent)}%
                    </strong>

                </div>

            </div>


            <div style="margin-top:15px">

                ${button}

            </div>

        </div>

    `;
}


// ========================================
// BUY AGAIN FROM SOLD POSITION
// ========================================

async function buyExistingStock(
    instrumentKey
) {

    const stock =
        liveStocks[instrumentKey];


    if (!stock) {

        showToast(
            "Live price not available"
        );

        return;
    }


    const quantity =
        prompt(
            `Enter quantity for ${stock.symbol}`
        );


    const qty =
        parseInt(quantity);


    if (!qty || qty <= 0) {

        return;
    }


    try {

        const response =
            await fetch(
                "/paper-trading/buy",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({

                        instrumentKey:
                            instrumentKey,

                        quantity:
                            qty

                    })
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            throw new Error(
                data.error ||
                "BUY failed"
            );

        }


        showToast(
            `${data.symbol} BUY executed at ₹${formatNumber(data.buyPrice)}`
        );


        await loadPositions();

    } catch (error) {

        showToast(
            error.message
        );

    }

}


// ========================================
// LIVE POSITION P/L UPDATE
// ========================================

function updatePositionLiveData(
    stock
) {

    const position =
        positions.find(
            p =>
                p.instrumentKey ===
                stock.instrumentKey
        );


    if (!position) {
        return;
    }


    if (
        position.status !==
        "BOUGHT"
    ) {
        return;
    }


    const currentPrice =
        stock.ltp;


    const pnl =
        (
            currentPrice -
            position.buyPrice
        ) *
        position.quantity;


    const pnlPercent =
        position.buyPrice !== 0
            ? (
                (
                    currentPrice -
                    position.buyPrice
                )
                /
                position.buyPrice
            ) * 100
            : 0;


    const card =
        document.getElementById(
            `position-${safeId(
                position.instrumentKey
            )}`
        );


    if (!card) {

        renderPositions();

        return;
    }


    const values =
        card.querySelectorAll(
            ".position-value"
        );


    // Current price
    if (values[1]) {

        values[1].innerHTML = `
            <span>Current Price</span>
            ₹${formatNumber(currentPrice)}
        `;

    }


    const pnlClass =
        pnl >= 0
            ? "positive"
            : "negative";


    // P/L
    if (values[3]) {

        values[3].innerHTML = `
            <span>Profit / Loss</span>
            <strong class="${pnlClass}">
                ${pnl >= 0 ? "+" : ""}
                ₹${formatNumber(pnl)}
            </strong>
        `;

    }


    // P/L %
    if (values[4]) {

        values[4].innerHTML = `
            <span>P/L %</span>
            <strong class="${pnlClass}">
                ${pnl >= 0 ? "+" : ""}
                ${formatNumber(pnlPercent)}%
            </strong>
        `;

    }

}


// ========================================
// HISTORY
// ========================================

async function loadHistory() {

    try {

        const response =
            await fetch(
                "/paper-trading/history"
            );

        const history =
            await response.json();

        renderHistory(history);

    } catch (error) {

        console.error(
            "History error:",
            error
        );

    }

}


// ========================================
// RENDER HISTORY
// ========================================

function renderHistory(history) {

    const table =
        document.getElementById(
            "historyTable"
        );


    // =====================================
    // CALCULATE SUMMARY
    // =====================================

    let totalTrades = history.length;

    let totalInvestment = 0;

    let totalProfitLoss = 0;


    history.forEach(trade => {

        const investment =
            Number(trade.buyPrice) *
            Number(trade.quantity);

        totalInvestment += investment;

        totalProfitLoss +=
            Number(trade.profitLoss);

    });


    let totalProfitLossPercent = 0;

    if (totalInvestment > 0) {

        totalProfitLossPercent =
            (
                totalProfitLoss /
                totalInvestment
            ) * 100;

    }


    // =====================================
    // UPDATE SUMMARY UI
    // =====================================

    document.getElementById(
        "totalTrades"
    ).innerText =
        totalTrades;


    document.getElementById(
        "totalInvestment"
    ).innerText =
        "₹" +
        formatNumber(totalInvestment);


    const pnlElement =
        document.getElementById(
            "totalProfitLoss"
        );


    pnlElement.innerText =
        (totalProfitLoss >= 0 ? "+" : "") +
        "₹" +
        formatNumber(
            totalProfitLoss
        );


    pnlElement.className =
        totalProfitLoss >= 0
            ? "profit"
            : "loss";


    const pnlPercentElement =
        document.getElementById(
            "totalProfitLossPercent"
        );


    pnlPercentElement.innerText =
        (totalProfitLossPercent >= 0
            ? "+"
            : "") +
        formatNumber(
            totalProfitLossPercent
        ) +
        "%";


    pnlPercentElement.className =
        totalProfitLossPercent >= 0
            ? "profit"
            : "loss";


    // =====================================
    // EMPTY HISTORY
    // =====================================

    if (!history.length) {

        table.innerHTML = `
            <tr>
                <td
                    colspan="9"
                    class="empty"
                >
                    No completed trades yet.
                </td>
            </tr>
        `;

        return;
    }


    // =====================================
    // HISTORY TABLE
    // =====================================

    table.innerHTML =
        history
            .map(trade => {

                const pnl =
                    Number(
                        trade.profitLoss
                    );


                const pnlPercent =
                    Number(
                        trade.profitLossPercent
                    );


                const pnlClass =
                    pnl >= 0
                        ? "positive"
                        : "negative";


                return `

                    <tr>

                        <td>
                            <strong>
                                ${escapeHtml(
                                    trade.symbol
                                )}
                            </strong>
                        </td>

                        <td>
                            ${trade.quantity}
                        </td>

                        <td>
                            ₹${formatNumber(
                                trade.buyPrice
                            )}
                        </td>

                        <td>
                            ₹${formatNumber(
                                trade.sellPrice
                            )}
                        </td>

                        <td
                            class="${pnlClass}"
                        >
                            ${pnl >= 0
                                ? "+"
                                : ""}
                            ₹${formatNumber(
                                pnl
                            )}
                        </td>

                        <td
                            class="${pnlClass}"
                        >
                            ${pnlPercent >= 0
                                ? "+"
                                : ""}
                            ${formatNumber(
                                pnlPercent
                            )}%
                        </td>

                        <td>
                            ${formatDate(
                                trade.buyDate
                            )}
                        </td>

                        <td>
                            ${formatDate(
                                trade.sellDate
                            )}
                        </td>

                        <td>

                            <button
                                class="delete-btn"
                                onclick="deleteHistory(
                                    ${trade.id}
                                )"
                            >
                                Delete
                            </button>

                        </td>

                    </tr>

                `;

            })
            .join("");

}


// ========================================
// DELETE HISTORY
// ========================================

async function deleteHistory(id) {

    if (
        !confirm(
            "Delete this completed trade?"
        )
    ) {

        return;
    }


    try {

        const response =
            await fetch(
                `/paper-trading/history/${id}`,
                {
                    method: "DELETE"
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            throw new Error(
                data.error ||
                "Delete failed"
            );

        }


        showToast(
            "Trade history deleted"
        );


        await loadHistory();

    } catch (error) {

        showToast(
            error.message
        );

    }

}


// ========================================
// HELPERS
// ========================================

function formatNumber(value) {

    if (
        value === null ||
        value === undefined ||
        Number.isNaN(Number(value))
    ) {

        return "0.00";
    }


    return Number(value)
        .toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );

}


function formatDate(value) {

    if (!value) {
        return "-";
    }


    const date =
        new Date(value);


    return date.toLocaleString(
        "en-IN"
    );

}


function safeId(value) {

    return String(value)
        .replace(/[^a-zA-Z0-9_-]/g, "-");

}


function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";
    }


    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

}


function escapeJs(value) {

    return String(value)
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'");
}


function showToast(message) {

    let toast =
        document.getElementById(
            "paperToast"
        );


    if (!toast) {

        toast =
            document.createElement(
                "div"
            );

        toast.id =
            "paperToast";


        toast.style.position =
            "fixed";

        toast.style.bottom =
            "25px";

        toast.style.right =
            "25px";

        toast.style.background =
            "#111827";

        toast.style.color =
            "white";

        toast.style.padding =
            "14px 20px";

        toast.style.borderRadius =
            "10px";

        toast.style.zIndex =
            "9999";

        toast.style.boxShadow =
            "0 10px 30px rgba(0,0,0,.2)";


        document.body.appendChild(
            toast
        );

    }


    toast.innerText = message;

    toast.style.display =
        "block";


    clearTimeout(
        toast.hideTimer
    );


    toast.hideTimer =
        setTimeout(() => {

            toast.style.display =
                "none";

        }, 3000);

}