package kz.whosnext.hr.documentservice.service;

import kz.whosnext.hr.documentservice.event.DocumentGeneratedEvent;
import kz.whosnext.hr.documentservice.event.DocumentSignedEvent;
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

    @Value("${app.kafka.topics.document-generated}") private String generatedTopic;
    @Value("${app.kafka.topics.document-signed}") private String signedTopic;

    public void sendDocumentGenerated(DocumentGeneratedEvent event) {
        send(generatedTopic, event.applicationId().toString(), event);
    }

    public void sendDocumentSigned(DocumentSignedEvent event) {
        send(signedTopic, event.applicationId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka [{}] ошибка: {}", topic, ex.getMessage());
                    else log.info("Kafka [{}] отправлено: key={}", topic, key);
                });
    }
}

