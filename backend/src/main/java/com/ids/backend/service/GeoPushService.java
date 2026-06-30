package com.ids.backend.service;

import com.ids.backend.model.AttackGeo;
import com.ids.backend.repository.AttackGeoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class GeoPushService {

    private static final Logger log = LoggerFactory.getLogger(GeoPushService.class);
    private final AttackGeoRepository geoRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private Instant lastCheck = Instant.now();

    public GeoPushService(AttackGeoRepository geoRepository, SimpMessagingTemplate messagingTemplate) {
        this.geoRepository = geoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void pushNewGeoLocations() {
        try {
            Instant since = lastCheck;
            lastCheck = Instant.now();
            List<AttackGeo> recent = geoRepository.findRecentSince(since);
            if (!recent.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/geo", recent);
                log.debug("Pushed {} new geo locations via WebSocket", recent.size());
            }
        } catch (Exception e) {
            log.error("Error pushing geo locations", e);
        }
    }
}
