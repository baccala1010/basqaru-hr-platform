package kz.whosnext.hr.candidate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.whosnext.hr.candidate.event.DocumentGeneratedEvent;
import kz.whosnext.hr.candidate.event.DocumentSignedEvent;
import kz.whosnext.hr.candidate.event.UserRegisteredEvent;
import kz.whosnext.hr.candidate.model.entity.Candidate;
import kz.whosnext.hr.candidate.model.enums.NotificationType;
import kz.whosnext.hr.candidate.repository.ApplicationRepository;
import kz.whosnext.hr.candidate.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @KafkaListener(topics = "${app.kafka.topics.user-registered}")
    @Transactional
    public void handleUserRegistered(String payload) {
        UserRegisteredEvent event = read(payload, UserRegisteredEvent.class);
        if (event == null || event.userId() == null) return;
        log.info("Kafka [user.registered]: userId={}, email={}", event.userId(), event.email());
        if (candidateRepository.existsByUserId(event.userId())) return;
        candidateRepository.save(Candidate.builder()
                .userId(event.userId())
                .email(event.email())
                .firstName(event.firstName())
                .lastName(event.lastName())
                .build());
    }

    @KafkaListener(topics = "${app.kafka.topics.document-generated}")
    @Transactional
    public void handleDocumentGenerated(String payload) {
        DocumentGeneratedEvent event = read(payload, DocumentGeneratedEvent.class);
        if (event == null || event.applicationId() == null) return;
        log.info("Kafka [document.generated]: applicationId={}", event.applicationId());
        applicationRepository.findById(event.applicationId()).ifPresent(app -> {
            app.setDocumentsReady(true);
            applicationRepository.save(app);
            notificationService.create(app.getCandidate().getUserId(),
                    "Документы сформированы",
                    "Документы по вашей заявке готовы к подписанию",
                    NotificationType.DOCUMENT_GENERATED, app.getId(), "APPLICATION");
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.document-signed}")
    @Transactional
    public void handleDocumentSigned(String payload) {
        DocumentSignedEvent event = read(payload, DocumentSignedEvent.class);
        if (event == null || event.applicationId() == null) return;
        log.info("Kafka [document.signed]: applicationId={}", event.applicationId());
        applicationRepository.findById(event.applicationId()).ifPresent(app -> {
            app.setDocumentsReady(true);
            app.setDocumentsSigned(true);
            applicationRepository.save(app);
            notificationService.create(app.getCandidate().getUserId(),
                    "Документы подписаны",
                    "Ваши документы успешно подписаны",
                    NotificationType.DOCUMENT_SIGNED, app.getId(), "APPLICATION");
        });
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception ex) {
            log.warn("Kafka payload parse failed for {}: {}", type.getSimpleName(), payload);
            return null;
        }
    }
}

