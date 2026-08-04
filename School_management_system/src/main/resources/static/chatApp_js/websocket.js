const WS_URL = "ws://localhost:8080/chat";

let socket;

function connectWebSocket() {

    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("✅ Connected to WebSocket Server");
    };

    socket.onmessage = (event) => {

        const chatMessage = JSON.parse(event.data);

        console.log("Received:", chatMessage);

        // Save into IndexedDB
        saveMessage(chatMessage);

        // Display on screen
        displayMessage(chatMessage);
    };

    socket.onclose = () => {

        console.log("❌ Connection Closed");

        // Auto reconnect after 3 seconds
        setTimeout(connectWebSocket, 3000);
    };

    socket.onerror = (error) => {
        console.error(error);
    };
}

const conversationId = "teacher_101";

function sendMessage(sender, message) {

    if (socket.readyState !== WebSocket.OPEN)
        return;

    const chatMessage = {
        id: crypto.randomUUID(),
        conversationId: conversationId,
        sender: sender,
        message: message,
        timestamp: Date.now(),
        status: "SENT"
    };

    socket.send(JSON.stringify(chatMessage));
}

// Connect automatically
connectWebSocket();