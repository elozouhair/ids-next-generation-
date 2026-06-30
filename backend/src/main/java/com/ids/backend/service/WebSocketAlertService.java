package com.ids.backend.service;

import com.ids.backend.model.Alert;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketAlertService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketAlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendAlert(Alert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    public void sendDashboardUpdate(Object stats) {
        messagingTemplate.convertAndSend("/topic/dashboard", stats);
    }
}
