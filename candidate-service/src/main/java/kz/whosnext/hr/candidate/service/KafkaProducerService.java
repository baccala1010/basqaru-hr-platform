package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.application-submitted}") private String submittedTopic;
    @Value("${app.kafka.topics.application-approved}") private String approvedTopic;
    @Value("${app.kafka.topics.application-status-changed}") private String statusChangedTopic;
    @Value("${app.kafka.topics.candidate-promoted}") private String promotedTopic;
    @Value("${app.kafka.topics.application-updated}") private String updatedTopic;

    public void sendApplicationSubmitted(ApplicationSubmittedEvent event) {
        send(submittedTopic, event.applicationId().toString(), event);
    }

    public void sendApplicationApproved(ApplicationApprovedEvent event) {
        send(approvedTopic, event.applicationId().toString(), event);
    }

    public void sendStatusChanged(ApplicationStatusChangedEvent event) {
        send(statusChangedTopic, event.applicationId().toString(), event);
    }

    public void sendCandidatePromoted(CandidatePromotedEvent event) {
        send(promotedTopic, event.userId().toString(), event);
    }

    public void sendApplicationUpdated(ApplicationUpdatedEvent event) {
        send(updatedTopic, event.applicationId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka [{}] ошибка: {}", topic, ex.getMessage());
                    else log.info("Kafka [{}] отправлено: key={}", topic, key);
                });
    }
}

