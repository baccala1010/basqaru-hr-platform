package kz.whosnext.hr.employeeservice.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("WebSocket подключён: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.has("type") ? node.get("type").asText() : "";
            if ("AUTH".equals(type)) {
                log.debug("WebSocket AUTH received for session: {}", session.getId());
                session.sendMessage(new TextMessage("{\"type\":\"AUTH_OK\"}"));
            }
        } catch (Exception e) {
            log.debug("Ошибка обработки WS-сообщения: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.debug("WebSocket отключён: {} ({})", session.getId(), status);
    }

    public void broadcast(String payload) {
        TextMessage msg = new TextMessage(payload);
        sessions.values().forEach(s -> {
            try {
                if (s.isOpen()) s.sendMessage(msg);
            } catch (IOException e) {
                log.debug("Не удалось отправить WS-сообщение: {}", e.getMessage());
            }
        });
    }
}

