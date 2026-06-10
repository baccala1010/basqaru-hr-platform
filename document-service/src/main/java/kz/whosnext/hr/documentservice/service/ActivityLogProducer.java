package kz.whosnext.hr.documentservice.service;

import kz.whosnext.hr.documentservice.event.ActivityEvent;
import kz.whosnext.hr.documentservice.model.enums.ActivityAction;
import kz.whosnext.hr.documentservice.model.enums.ActivitySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityLogProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.activity-log}")
    private String activityLogTopic;

    public void log(ActivityAction action, ActivitySource source,
                    UUID actorId, String actorEmail, String actorRole,
                    String entityType, String entityId, String details) {
        ActivityEvent event = new ActivityEvent(
                actorId, actorEmail, actorRole,
                action, source, entityType, entityId,
                details, Instant.now()
        );
        kafkaTemplate.send(activityLogTopic, actorId != null ? actorId.toString() : "SYSTEM", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send activity event: action={}, actorId={}, error={}",
                                action, actorId, ex.getMessage());
                    }
                });
    }
}
