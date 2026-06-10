package kz.whosnext.hr.auth.service;

import kz.whosnext.hr.auth.event.UserRegisteredEvent;
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

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(userRegisteredTopic, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Ошибка отправки Kafka [{}]: {}", userRegisteredTopic, ex.getMessage());
                    } else {
                        log.info("Kafka [{}]: userId={}", userRegisteredTopic, event.userId());
                    }
                });
    }
}

