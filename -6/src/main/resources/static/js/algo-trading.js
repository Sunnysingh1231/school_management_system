let historicalLiveSocket = null;

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

    historicalLiveSocket.onopen = function () {
        console.log("=================================");
        console.log("HISTORICAL LIVE SOCKET CONNECTED");
        console.log("=================================");
    };

    historicalLiveSocket.onmessage = function (event) {

        try {

            const data = JSON.parse(event.data);

            
            console.log(data);

            // Agar backend Map<String, MarketTick> bhej raha hai
            

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

    historicalLiveSocket.onerror = function (error) {
        console.error(
            "HISTORICAL LIVE SOCKET ERROR:",
            error
        );
    };

    historicalLiveSocket.onclose = function () {

        console.log(
            "HISTORICAL LIVE SOCKET CLOSED"
        );

        // reconnect
        setTimeout(
            connectHistoricalLiveWebSocket,
            3000
        );
    };
}


// Start WebSocket
connectHistoricalLiveWebSocket();