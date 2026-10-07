let chart = null;
let candleSeries = null;

let ema20Series = null;
let ema50Series = null;
let ema200Series = null;

/* ================================================= */
/* INITIALIZE CHART */
/* ================================================= */

function initializeChart() {

const container = document.getElementById("chart");

chart = LightweightCharts.createChart(
    container,
    {
        width: container.clientWidth,
        height: 650,

        layout: {
            background: {
                color: "#111827"
            },
            textColor: "#9ca3af"
        },

        grid: {
            vertLines: {
                color: "#1f2937"
            },
            horzLines: {
                color: "#1f2937"
            }
        },

        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal
        },

        rightPriceScale: {
            borderColor: "#374151"
        },

        timeScale: {
            borderColor: "#374151",
            timeVisible: true,
            secondsVisible: false
        }
    }
);


/* ================================================= */
/* CANDLESTICK */
/* ================================================= */

candleSeries = chart.addSeries(
    LightweightCharts.CandlestickSeries,
    {
        upColor: "#22c55e",
        downColor: "#ef4444",
        borderVisible: false,
        wickUpColor: "#22c55e",
        wickDownColor: "#ef4444"
    }
);


/* ================================================= */
/* EMA 20 */
/* ================================================= */

ema20Series = chart.addSeries(
    LightweightCharts.LineSeries,
    {
        color: "#3b82f6",
        lineWidth: 2,
        title: "EMA 20",
        priceLineVisible: false,
        lastValueVisible: true
    }
);


/* ================================================= */
/* EMA 50 */
/* ================================================= */

ema50Series = chart.addSeries(
    LightweightCharts.LineSeries,
    {
        color: "#f59e0b",
        lineWidth: 2,
        title: "EMA 50",
        priceLineVisible: false,
        lastValueVisible: true
    }
);


/* ================================================= */
/* EMA 200 */
/* ================================================= */

ema200Series = chart.addSeries(
    LightweightCharts.LineSeries,
    {
        color: "#a855f7",
        lineWidth: 2,
        title: "EMA 200",
        priceLineVisible: false,
        lastValueVisible: true
    }
);


/* ================================================= */
/* RESIZE */
/* ================================================= */

window.addEventListener(
    "resize",
    () => {

        chart.applyOptions({
            width: container.clientWidth
        });

    }
);


}

/* ================================================= */
/* LOAD STOCK LIST */
/* ================================================= */

async function loadStocks() {

try {

    console.log("Calling stock API...");

    const response = await fetch(
        "/api/nifty50/stocks"
    );

    console.log(
        "Stock API status:",
        response.status
    );

    if (!response.ok) {

        throw new Error(
            "Stock API failed: " +
            response.status
        );
    }

    const stocks = await response.json();

    console.log(
        "Stocks received:",
        stocks
    );

    const select =
        document.getElementById("stockSelect");

    if (!select) {

        console.error(
            "stockSelect element not found"
        );

        return;
    }

    select.innerHTML =
        '<option value="">Select Stock</option>';


    Object.entries(stocks).forEach(
        ([instrumentKey, symbol]) => {

            const option =
                document.createElement("option");

            option.value = instrumentKey;
            option.textContent = symbol;

            select.appendChild(option);

        }
    );


    console.log(
        "Total stocks loaded:",
        Object.keys(stocks).length
    );

} catch (error) {

    console.error(
        "Stock loading error:",
        error
    );

}


}

/* ================================================= */
/* CALCULATE EMA SERIES */
/* Same logic as Java TrendFilterService */
/* ================================================= */

function calculateEMA(prices, period) {

const result = [];

if (prices.length < period) {
    return result;
}


/* ================================================ */
/* INITIAL SMA */
/* ================================================ */

let sum = 0;

for (let i = 0; i < period; i++) {

    sum += prices[i];

}

let ema = sum / period;


/* First EMA point */

result.push({
    index: period - 1,
    value: ema
});


/* ================================================ */
/* EMA MULTIPLIER */
/* ================================================ */

const multiplier =
    2 / (period + 1);


/* ================================================ */
/* REMAINING CANDLES */
/* ================================================ */

for (
    let i = period;
    i < prices.length;
    i++
) {

    const price = prices[i];

    ema =
        (price - ema) * multiplier +
        ema;


    result.push({
        index: i,
        value: ema
    });

}

return result;


}

/* ================================================= */
/* LOAD CANDLE DATA */
/* ================================================= */

async function loadChartData() {

const select =
    document.getElementById("stockSelect");

const instrumentKey =
    select.value;


if (!instrumentKey) {

    alert(
        "Please select a stock"
    );

    return;
}


const symbol =
    select.options[
        select.selectedIndex
    ].textContent;


try {

    document.getElementById(
        "loadBtn"
    ).textContent = "Loading...";


    /* ============================================ */
    /* GET CANDLES */
    /* ============================================ */

    const response =
        await fetch(
            "/api/nifty50/candles?instrumentKey=" +
            encodeURIComponent(
                instrumentKey
            )
        );


    if (!response.ok) {

        throw new Error(
            "Candle API failed"
        );

    }


    const candles =
        await response.json();


    if (!candles || candles.length === 0) {

        throw new Error(
            "No candle data received"
        );

    }


    /* ============================================ */
    /* CHART CANDLE DATA */
    /* ============================================ */

    const chartData =
        candles.map(candle => {

            const date =
                new Date(
                    candle.timestamp
                );


            const istDate =
                new Date(
                    date.toLocaleString(
                        "en-US",
                        {
                            timeZone:
                                "Asia/Kolkata"
                        }
                    )
                );


            const timestamp =
                Math.floor(
                    istDate.getTime() / 1000
                );


            return {

                time: timestamp,

                open:
                    Number(candle.open),

                high:
                    Number(candle.high),

                low:
                    Number(candle.low),

                close:
                    Number(candle.close)

            };

        });


    /* ============================================ */
    /* SORT DATA */
    /* ============================================ */

    chartData.sort(
        (a, b) =>
            a.time - b.time
    );


    /* ============================================ */
    /* REMOVE DUPLICATE TIMES */
    /* ============================================ */

    const uniqueChartData = [];

    const usedTimes = new Set();


    chartData.forEach(candle => {

        if (!usedTimes.has(candle.time)) {

            usedTimes.add(candle.time);

            uniqueChartData.push(candle);

        }

    });


    /* ============================================ */
    /* SET CANDLE DATA */
    /* ============================================ */

    candleSeries.setData(
        uniqueChartData
    );


    /* ============================================ */
    /* CLOSE PRICES */
    /* ============================================ */

    const closePrices =
        uniqueChartData.map(
            candle => candle.close
        );


    /* ============================================ */
    /* EMA 20 */
    /* ============================================ */

    const ema20 =
        calculateEMA(
            closePrices,
            20
        );


    /* ============================================ */
    /* EMA 50 */
    /* ============================================ */

    const ema50 =
        calculateEMA(
            closePrices,
            50
        );


    /* ============================================ */
    /* EMA 200 */
    /* ============================================ */

    const ema200 =
        calculateEMA(
            closePrices,
            200
        );


    /* ============================================ */
    /* CONVERT EMA TO CHART DATA */
    /* ============================================ */

    const ema20Chart =
        ema20.map(point => {

            return {

                time:
                    uniqueChartData[
                        point.index
                    ].time,

                value:
                    point.value

            };

        });


    const ema50Chart =
        ema50.map(point => {

            return {

                time:
                    uniqueChartData[
                        point.index
                    ].time,

                value:
                    point.value

            };

        });


    const ema200Chart =
        ema200.map(point => {

            return {

                time:
                    uniqueChartData[
                        point.index
                    ].time,

                value:
                    point.value

            };

        });


    /* ============================================ */
    /* SET EMA LINES */
    /* ============================================ */

    ema20Series.setData(
        ema20Chart
    );

    ema50Series.setData(
        ema50Chart
    );

    ema200Series.setData(
        ema200Chart
    );


    /* ============================================ */
    /* FIT CHART */
    /* ============================================ */

    chart.timeScale()
        .fitContent();


    /* ============================================ */
    /* UPDATE UI */
    /* ============================================ */

    document.getElementById(
        "selectedStock"
    ).textContent =
        symbol;


    document.getElementById(
        "chartTitle"
    ).textContent =
        symbol +
        " - 5 Minute";


    document.getElementById(
        "candleCount"
    ).textContent =
        uniqueChartData.length;


    console.log(
        "Candles:",
        uniqueChartData.length
    );

    console.log(
        "EMA20:",
        ema20.length
    );

    console.log(
        "EMA50:",
        ema50.length
    );

    console.log(
        "EMA200:",
        ema200.length
    );


    /* ============================================ */
    /* OPTIONAL: GET BACKEND CURRENT TREND */
    /* ============================================ */

    try {

        const trendResponse =
            await fetch(
                "/api/nifty50/trend?instrumentKey=" +
                encodeURIComponent(
                    instrumentKey
                )
            );


        if (trendResponse.ok) {

            const trend =
                await trendResponse.json();

            console.log(
                "Backend Trend:",
                trend
            );

            console.log(
                "Backend EMA20:",
                trend.ema20
            );

            console.log(
                "Backend EMA50:",
                trend.ema50
            );

            console.log(
                "Backend EMA200:",
                trend.ema200
            );

            console.log(
                "Trend:",
                trend.trend
            );

        }

    } catch (trendError) {

        console.error(
            "Trend API error:",
            trendError
        );

    }


} catch (error) {

    console.error(
        "Chart data error:",
        error
    );

    alert(
        "Candle data load nahi hua."
    );

} finally {

    document.getElementById(
        "loadBtn"
    ).textContent =
        "Load Chart";

}


}

/* ================================================= */
/* START */
/* ================================================= */

document.addEventListener(
"DOMContentLoaded",
() => {

    initializeChart();

    loadStocks();

    document
        .getElementById("loadBtn")
        .addEventListener(
            "click",
            loadChartData
        );

}


);
