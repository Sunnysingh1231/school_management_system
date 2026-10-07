package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LivePriceService {

    private final ObjectMapper objectMapper;

    private final Map<String, LivePrice> prices =
            new ConcurrentHashMap<>();

    private final Map<String, WebSocketSession> sessions =
            new ConcurrentHashMap<>();


    public LivePriceService(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }


    public void updatePrice(LivePrice livePrice) {

        prices.put(
                livePrice.getSymbol(),
                livePrice
        );

        broadcast(livePrice);
        System.out.println(livePrice);
    }


    public Map<String, LivePrice> getPrices() {

        return prices;
    }


    public void addSession(
            WebSocketSession session) {

        sessions.put(
                session.getId(),
                session
        );
    }


    public void removeSession(
            WebSocketSession session) {

        sessions.remove(
                session.getId()
        );
    }


    private void broadcast(
            LivePrice livePrice) {

        try {

            String json =
                    objectMapper.writeValueAsString(
                            livePrice
                    );

            TextMessage message =
                    new TextMessage(json);

            sessions.values().forEach(session -> {

                try {

                    if (session.isOpen()) {

                        session.sendMessage(message);
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                }

            });

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}