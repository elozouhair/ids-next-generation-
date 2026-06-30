package com.ids.backend.config;

import com.ids.backend.repository.AlertRepository;
import com.ids.backend.service.WebSocketAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertConsumer.class);

    public KafkaAlertConsumer(AlertRepository alertRepository,
                              WebSocketAlertService webSocketService) {
        log.info("Backend Kafka consumer disabled - Spark writes directly to PostgreSQL");
    }
}
