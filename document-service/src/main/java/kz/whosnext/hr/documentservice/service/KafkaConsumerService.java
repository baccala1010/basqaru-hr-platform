package kz.whosnext.hr.documentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.whosnext.hr.documentservice.event.ApplicationApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ContractService contractService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${app.kafka.topics.application-approved}")
    public void handleApplicationApproved(String payload) {
        ApplicationApprovedEvent event = read(payload, ApplicationApprovedEvent.class);
        log.info("Kafka [application.approved]: applicationId={}, candidateId={}",
                event.applicationId(), event.candidateId());
        contractService.generatePackage(
                event.applicationId(),
                event.candidateId(),
                event.vacancyId(),
                event.firstName(),
                event.lastName(),
                event.iin(),
                null,
                null,
                event.userId()
        );
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception ex) {
            log.warn("Kafka payload parse failed for {}: {}", type.getSimpleName(), payload);
            throw new IllegalArgumentException("Kafka payload parse failed for " + type.getSimpleName(), ex);
        }
    }
}

