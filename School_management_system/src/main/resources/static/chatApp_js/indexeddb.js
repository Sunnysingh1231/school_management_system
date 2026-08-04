const DB_NAME = "ChatDB";
const DB_VERSION = 1;
const STORE_NAME = "messages";

let db;

console.log("All fine...")

// Open Database
function openDatabase() {

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = function (event) {

        db = event.target.result;

        if (!db.objectStoreNames.contains(STORE_NAME)) {

			db.createObjectStore(STORE_NAME, {
			    keyPath: "id"
			});

        }
    };

    request.onsuccess = function (event) {
        db = event.target.result;

        console.log("IndexedDB Connected");

        loadMessages();
    };

    request.onerror = function (event) {
        console.error("IndexedDB Error:", event.target.error);
    };
}

// Save Message
function saveMessage(chatMessage) {

    const transaction = db.transaction(STORE_NAME, "readwrite");
    const store = transaction.objectStore(STORE_NAME);

    store.put(chatMessage);
}

// Load All Messages
function loadMessages() {

    chatBody.innerHTML = "";

    const transaction = db.transaction(STORE_NAME, "readonly");
    const store = transaction.objectStore(STORE_NAME);

    const request = store.getAll();

    request.onsuccess = function () {

        request.result
            .filter(msg => msg.conversationId === conversationId)
            .sort((a, b) => a.timestamp - b.timestamp)
            .forEach(displayMessage);

        chatBody.scrollTop = chatBody.scrollHeight;
    };
}

// Open DB automatically
openDatabase();