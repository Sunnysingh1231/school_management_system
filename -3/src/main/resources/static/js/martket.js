
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
   CONNECT
===================================================== */

function connectMarket() {

    const protocol =
        window.location.protocol === "https:"
            ? "wss"
            : "ws";

    const url =
        protocol +
        "://" +
        window.location.host +
        "/ws/market";

    console.log("Connecting:", url);

    socket = new WebSocket(url);

    socket.onopen = function() {

        console.log("Browser WebSocket connected");

        setConnection(true, "Live");
    };

    socket.onmessage = function(event) {

        try {

            const data = JSON.parse(event.data);

            //console.log("MARKET DATA:", data);

            processMarketData(data);

            const now = new Date();

            const lastUpdate2 =
                document.getElementById("lastUpdate2");

            if (lastUpdate2) {

                lastUpdate2.textContent =
                    now.toLocaleTimeString("en-IN", {
                        hour: "2-digit",
                        minute: "2-digit",
                        second: "2-digit",
                        hour12: false
                    });
            }

        } catch (error) {

            console.error(
                "Invalid market data:",
                error
            );
        }
    };

    socket.onclose = function() {

        console.log("WebSocket closed");

        setConnection(
            false,
            "Disconnected"
        );

        setTimeout(
            connectMarket,
            3000
        );
    };

    socket.onerror = function(error) {

        console.error(
            "WebSocket error:",
            error
        );
    };
}

/* =====================================================
   CONNECTION STATUS
===================================================== */

function setConnection(online, text) {

    const dot =
        document.getElementById("connectionDot");

    const status =
        document.getElementById("connectionText");

    if (dot) {

        if (online) {

            dot.classList.remove("offline");
            dot.classList.add("online");

        } else {

            dot.classList.remove("online");
            dot.classList.add("offline");
        }
    }

    if (status) {

        status.textContent = text;
    }
}

/* =====================================================
   PROCESS DATA
===================================================== */

function processMarketData(data) {

    if (!data) {
        return;
    }

    if (!data.instrumentKey) {
        return;
    }

    if (data.symbol) {

        const marketStatus =
            document.getElementById("marketStatus");

        if (marketStatus) {

            marketStatus.className = "positive";
            marketStatus.textContent = "Live";
        }

        /* Store complete live data */

        marketData[data.instrumentKey] = data;

        /* Update NIFTY / BANK */

        updateCards(data);

        /* Update selected stock depth */

        if (
            selectedInstrumentKey ===
            data.instrumentKey
        ) {

            updateDepthForStock(data);
        }

        /* Add stock to depth dropdown */

        addDepthSymbol(data.symbol);

        /* First live data */

        if (!tableInitialized) {

            tableInitialized = true;

            renderTable();

        } else {

            updateVisibleTable(data);
        }

        /* Last update */

        const lastUpdate =
            document.getElementById("lastUpdate");

        if (lastUpdate) {

            lastUpdate.textContent =
                new Date().toLocaleTimeString();
        }
    }
}

/* =====================================================
   UPDATE ONLY VISIBLE TABLE
===================================================== */

function updateVisibleTable(data) {

    if (!data || !data.instrumentKey) {
        return;
    }

    if (
        searchText &&
        (
            !data.symbol ||
            !data.symbol
                .toLowerCase()
                .includes(
                    searchText.toLowerCase()
                )
        )
    ) {

        return;
    }

    const stocks =
        getFilteredStocks();

    const index =
        stocks.findIndex(
            item =>
                item.instrumentKey ===
                data.instrumentKey
        );

    if (index === -1) {

        renderTable();

        return;
    }

    const page =
        Math.floor(
            index / STOCKS_PER_PAGE
        ) + 1;

    if (page !== currentPage) {
        return;
    }

    updateTableRow(
        data,
        index
    );
}

/* =====================================================
   GET FILTERED STOCKS
===================================================== */

function getFilteredStocks() {

    let stocks =
        Object.values(marketData);

    if (searchText.trim() !== "") {

        const query =
            searchText
                .trim()
                .toLowerCase();

        stocks =
            stocks.filter(
                stock =>
                    stock.symbol &&
                    stock.symbol
                        .toLowerCase()
                        .includes(query)
            );
    }

    stocks.sort(
        (a, b) => {

            return (
                a.symbol || ""
            ).localeCompare(
                b.symbol || ""
            );
        }
    );

    if (selectedInstrumentKey) {

        const selectedIndex =
            stocks.findIndex(
                stock =>
                    stock.instrumentKey ===
                    selectedInstrumentKey
            );

        if (selectedIndex > 0) {

            const selected =
                stocks.splice(
                    selectedIndex,
                    1
                )[0];

            stocks.unshift(selected);
        }
    }

    return stocks;
}

/* =====================================================
   RENDER TABLE
===================================================== */

function renderTable() {

    const table =
        document.getElementById(
            "marketTable"
        );

    if (!table) {
        return;
    }

    table.innerHTML = "";

    const stocks =
        getFilteredStocks();

    const totalStocks =
        stocks.length;

    const totalPages =
        Math.max(
            1,
            Math.ceil(
                totalStocks /
                STOCKS_PER_PAGE
            )
        );

    if (
        currentPage >
        totalPages
    ) {

        currentPage =
            totalPages;
    }

    const start =
        (
            currentPage - 1
        ) *
        STOCKS_PER_PAGE;

    const end =
        Math.min(
            start +
            STOCKS_PER_PAGE,
            totalStocks
        );

    const visibleStocks =
        stocks.slice(
            start,
            end
        );

    visibleStocks.forEach(
        (
            data,
            index
        ) => {

            const row =
                document.createElement("tr");

            row.className =
                "market-row";

            row.dataset.instrumentKey =
                data.instrumentKey;

            row.addEventListener(
                "click",
                function() {

                    selectStock(
                        data.instrumentKey
                    );
                }
            );

            updateTableRowHTML(
                row,
                data,
                start + index + 1
            );

            table.appendChild(row);
        }
    );

    updatePagination(
        totalStocks,
        start,
        end
    );
}

/* =====================================================
   UPDATE TABLE ROW
===================================================== */

function updateTableRow(
    data,
    index
) {

    const rows =
        document.querySelectorAll(
            "#marketTable tr"
        );

    const start =
        (
            currentPage - 1
        ) *
        STOCKS_PER_PAGE;

    const localIndex =
        index - start;

    if (
        localIndex < 0 ||
        localIndex >= rows.length
    ) {

        renderTable();

        return;
    }

    updateTableRowHTML(
        rows[localIndex],
        data,
        index + 1
    );
}

/* =====================================================
   TABLE ROW HTML
===================================================== */

function updateTableRowHTML(
    row,
    data,
    serial
) {

    const changeClass =
        Number(data.change || 0) >= 0
            ? "positive"
            : "negative";

    row.innerHTML = `

        <td>
            ${serial}
        </td>

        <td>
            <strong>
                ${data.symbol || "--"}
            </strong>
        </td>

        <td>
            ₹${format(data.ltp)}
        </td>

        <td class="${changeClass}">
            ${formatSigned(data.change)}
        </td>

        <td class="${changeClass}">
            ${formatSigned(data.changePercent)}%
        </td>

        <td>
            ${format(data.open)}
        </td>

        <td>
            ${format(data.high)}
        </td>

        <td>
            ${format(data.low)}
        </td>

        <td>
            ${format(data.previousClose)}
        </td>

        <td>
            ${formatNumber(data.volume)}
        </td>

        <td>
            ${formatNumber(data.lastQuantity)}
        </td>

        <td class="positive">
            ${formatNumber(data.totByQ)}
        </td>

        <td class="negative">
            ${formatNumber(data.totSlQ)}
        </td>
    `;

    row.classList.toggle(
        "selected-stock",
        data.instrumentKey ===
        selectedInstrumentKey
    );
}

/* =====================================================
   PAGINATION
===================================================== */

function updatePagination(
    totalStocks,
    start,
    end
) {

    const pagination =
        document.getElementById(
            "pagination"
        );

    const pageInfo =
        document.getElementById(
            "pageInfo"
        );

    const totalStocksElement =
        document.getElementById(
            "totalStocks"
        );

    if (
        !pagination ||
        !pageInfo ||
        !totalStocksElement
    ) {

        return;
    }

    totalStocksElement.textContent =
        totalStocks;

    if (totalStocks === 0) {

        pageInfo.textContent =
            "0 - 0";

        pagination.innerHTML =
            "";

        return;
    }

    pageInfo.textContent =
        `${start + 1} - ${end}`;

    const totalPages =
        Math.ceil(
            totalStocks /
            STOCKS_PER_PAGE
        );

    pagination.innerHTML =
        "";

    /* Previous */

    const previous =
        document.createElement("button");

    previous.textContent =
        "‹ Previous";

    previous.disabled =
        currentPage === 1;

    previous.addEventListener(
        "click",
        function() {

            if (
                currentPage > 1
            ) {

                currentPage--;

                renderTable();

                scrollToMarketTable();
            }
        }
    );

    pagination.appendChild(
        previous
    );

    /* Page buttons */

    const maxButtons = 7;

    let startPage =
        Math.max(
            1,
            currentPage -
            Math.floor(
                maxButtons / 2
            )
        );

    let endPage =
        Math.min(
            totalPages,
            startPage +
            maxButtons -
            1
        );

    if (
        endPage -
        startPage +
        1 <
        maxButtons
    ) {

        startPage =
            Math.max(
                1,
                endPage -
                maxButtons +
                1
            );
    }

    for (
        let page = startPage;
        page <= endPage;
        page++
    ) {

        const button =
            document.createElement(
                "button"
            );

        button.textContent =
            page;

        if (
            page === currentPage
        ) {

            button.classList.add(
                "active"
            );
        }

        button.addEventListener(
            "click",
            function() {

                currentPage =
                    page;

                renderTable();

                scrollToMarketTable();
            }
        );

        pagination.appendChild(
            button
        );
    }

    /* Next */

    const next =
        document.createElement("button");

    next.textContent =
        "Next ›";

    next.disabled =
        currentPage === totalPages;

    next.addEventListener(
        "click",
        function() {

            if (
                currentPage <
                totalPages
            ) {

                currentPage++;

                renderTable();

                scrollToMarketTable();
            }
        }
    );

    pagination.appendChild(
        next
    );
}

/* =====================================================
   SELECT STOCK
===================================================== */

function selectStock(
    instrumentKey
) {

    const stock =
        marketData[instrumentKey];

    if (!stock) {
        return;
    }

    selectedInstrumentKey =
        instrumentKey;

    const search =
        document.getElementById(
            "stockSearch"
        );

    if (search) {

        search.value =
            stock.symbol || "";

        searchText =
            stock.symbol || "";
    }

    currentPage = 1;

    renderTable();

    const select =
        document.getElementById(
            "depthSymbolSelect"
        );

    if (select) {

        select.value =
            stock.symbol || "";
    }

    updateDepthForStock(
        stock
    );

    scrollToMarketTable();
}

/* =====================================================
   SEARCH
===================================================== */

function setupSearch() {

    const search =
        document.getElementById(
            "stockSearch"
        );

    const clear =
        document.getElementById(
            "clearSearch"
        );

    if (!search) {
        return;
    }

    search.addEventListener(
        "input",
        function() {

            searchText =
                search.value.trim();

            currentPage = 1;

            selectedInstrumentKey =
                null;

            renderTable();
        }
    );

    if (clear) {

        clear.addEventListener(
            "click",
            function() {

                search.value =
                    "";

                searchText =
                    "";

                selectedInstrumentKey =
                    null;

                currentPage = 1;

                renderTable();

                search.focus();
            }
        );
    }
}

/* =====================================================
   SCROLL TABLE
===================================================== */

function scrollToMarketTable() {

    const table =
        document.getElementById(
            "marketTable"
        );

    if (!table) {
        return;
    }

    const panel =
        table.closest(".panel");

    if (panel) {

        panel.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }
}

/* =====================================================
   NIFTY / BANK CARDS
===================================================== */

function updateCards(data) {

    if (
        data.instrumentKey ===
        "NSE_INDEX|Nifty 50"
    ) {

        updateCard(
            "nifty",
            data
        );
    }

    if (
        data.instrumentKey ===
        "NSE_INDEX|Nifty Bank"
    ) {

        updateCard(
            "bank",
            data
        );
    }
}

function updateCard(
    prefix,
    data
) {

    const price =
        document.getElementById(
            prefix + "Price"
        );

    if (price) {

        price.textContent =
            "₹" +
            format(data.ltp);
    }

    const change =
        document.getElementById(
            prefix + "Change"
        );

    if (change) {

        change.textContent =
            formatSigned(data.change) +
            " (" +
            formatSigned(data.changePercent) +
            "%)";

        change.classList.remove(
            "up",
            "down"
        );

        change.classList.add(
            Number(data.change || 0) >= 0
                ? "up"
                : "down"
        );
    }

    const open =
        document.getElementById(
            prefix + "Open"
        );

    if (open) {

        open.textContent =
            format(data.open);
    }

    const high =
        document.getElementById(
            prefix + "High"
        );

    if (high) {

        high.textContent =
            format(data.high);
    }

    const low =
        document.getElementById(
            prefix + "Low"
        );

    if (low) {

        low.textContent =
            format(data.low);
    }

    const close =
        document.getElementById(
            prefix + "Close"
        );

    if (close) {

        close.textContent =
            format(
                data.previousClose
            );
    }
}

/* =====================================================
   DEPTH SYMBOL DROPDOWN
===================================================== */

function addDepthSymbol(
    symbol
) {

    if (!symbol) {
        return;
    }

    if (
        depthSymbol.has(symbol)
    ) {

        return;
    }

    depthSymbol.add(symbol);

    const select =
        document.getElementById(
            "depthSymbolSelect"
        );

    if (!select) {
        return;
    }

    const option =
        document.createElement(
            "option"
        );

    option.value =
        symbol;

    option.textContent =
        symbol;

    select.appendChild(
        option
    );
}

/* =====================================================
   DEPTH DROPDOWN CHANGE
===================================================== */

function setupDepthDropdown() {

    const select =
        document.getElementById(
            "depthSymbolSelect"
        );

    if (!select) {
        return;
    }

    select.addEventListener(
        "change",
        function() {

            const symbol =
                select.value;

            if (!symbol) {
                return;
            }

            const stock =
                Object.values(
                    marketData
                ).find(
                    item =>
                        item.symbol ===
                        symbol
                );

            if (!stock) {
                return;
            }

            selectedInstrumentKey =
                stock.instrumentKey;

            const depthSymbolElement =
                document.getElementById(
                    "depthSymbol"
                );

            if (depthSymbolElement) {

                depthSymbolElement.textContent =
                    stock.symbol;
            }

            updateDepthForStock(
                stock
            );

            selectStock(
                stock.instrumentKey
            );
        }
    );
}

/* =====================================================
   UPDATE DEPTH
===================================================== */

function updateDepthForStock(
    data
) {

    if (!data) {
        return;
    }


    const depthSymbolElement =
        document.getElementById(
            "depthSymbol"
        );

    if (depthSymbolElement) {

        depthSymbolElement.textContent =
            data.symbol || "--";
    }

    totBidAsk(data);

    updateBidLevels(
        data.bidLevels
    );

    updateAskLevels(
        data.askLevels
    );
}

/* =====================================================
   TOTAL BID / ASK
===================================================== */

function totBidAsk(data) {


    const totalBidElement =
        document.getElementById(
            "totalBidQty"
        );

    if (totalBidElement) {

        totalBidElement.textContent =
            formatNumber(
                data.totByQ

            );
    }


    const totalAskElement =
        document.getElementById(
            "totalAskQty"
        );

    if (totalAskElement) {

        totalAskElement.textContent =
            formatNumber(
                data.totSlQ
            );
    }
}

/* =====================================================
   BID LEVELS
===================================================== */

function updateBidLevels(
    bids
) {

    const container =
        document.getElementById(
            "bidLevels"
        );

    if (!container) {
        return;
    }

    container.innerHTML = "";

    bids =
        Array.isArray(bids)
            ? bids
            : [];

    for (
        let i = 0;
        i < 5;
        i++
    ) {

        const level =
            bids[i];

        const row =
            document.createElement(
                "div"
            );

        row.className =
            "depth-row bid-row";

        if (level) {

            row.innerHTML = `

                <strong>
                    ₹${format(level.price)}
                </strong>

                <span>
                    ${formatNumber(
                level.quantity
            )}
                </span>

            `;

        } else {

            row.innerHTML = `

                <strong>--</strong>

                <span>--</span>

            `;
        }

        container.appendChild(
            row
        );
    }
}

/* =====================================================
   ASK LEVELS
===================================================== */

function updateAskLevels(
    asks
) {

    const container =
        document.getElementById(
            "askLevels"
        );

    if (!container) {
        return;
    }

    container.innerHTML = "";

    asks =
        Array.isArray(asks)
            ? asks
            : [];

    for (
        let i = 0;
        i < 5;
        i++
    ) {

        const level =
            asks[i];

        const row =
            document.createElement(
                "div"
            );

        row.className =
            "depth-row ask-row";

        if (level) {

            row.innerHTML = `

                <strong>
                    ₹${format(level.price)}
                </strong>

                <span>
                    ${formatNumber(
                level.quantity
            )}
                </span>

            `;

        } else {

            row.innerHTML = `

                <strong>--</strong>

                <span>--</span>

            `;
        }

        container.appendChild(
            row
        );
    }
}

/* =====================================================
   HELPERS
===================================================== */

function format(value) {

    if (
        value === null ||
        value === undefined ||
        isNaN(value)
    ) {

        return "--";
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

function formatSigned(value) {

    if (
        value === null ||
        value === undefined ||
        isNaN(value)
    ) {

        return "--";
    }

    const number =
        Number(value);

    return (
        number >= 0
            ? "+"
            : ""
    ) +
        number.toFixed(2);
}

function formatNumber(value) {

    if (
        value === null ||
        value === undefined ||
        isNaN(value)
    ) {

        return "--";
    }

    return Number(value)
        .toLocaleString("en-IN");
}

/* =====================================================
   INITIALIZE
===================================================== */

document.addEventListener(
    "DOMContentLoaded",
    function() {

        console.log(
            "========== market.js LOADED =========="
        );

        console.log(
            "========== DOM READY =========="
        );

        setupSearch();

        setupDepthDropdown();

        connectMarket();
    }
);