package kz.whosnext.hr.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kz.whosnext.hr.auth.event.ActivityEvent;
import kz.whosnext.hr.auth.model.entity.ActivityLog;
import kz.whosnext.hr.auth.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogConsumerService {

    private final ActivityLogRepository activityLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @KafkaListener(topics = "${app.kafka.topics.activity-log}", groupId = "auth-service-group")
    @Transactional
    public void consumeActivityEvent(String payload) {
        ActivityEvent event = parseEvent(payload);
        if (event == null) {
            return;
        }

        ActivityLog logEntry = ActivityLog.builder()
                .actorId(event.actorId())
                .actorEmail(event.actorEmail())
                .actorRole(event.actorRole())
                .action(event.action())
                .source(event.source())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .details(event.details())
                .createdAt(event.timestamp() != null
                        ? LocalDateTime.ofInstant(event.timestamp(), ZoneOffset.UTC)
                        : LocalDateTime.now())
                .build();

        activityLogRepository.save(logEntry);
        log.debug("Activity log saved: action={}, actorId={}", event.action(), event.actorId());
    }

    private ActivityEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, ActivityEvent.class);
        } catch (Exception ex) {
            log.warn("Failed to parse ActivityEvent payload: {}", ex.getMessage());
            return null;
        }
    }
}
