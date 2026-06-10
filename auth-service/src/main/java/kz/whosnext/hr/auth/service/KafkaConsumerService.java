package kz.whosnext.hr.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.whosnext.hr.auth.event.CandidatePromotedEvent;
import kz.whosnext.hr.auth.event.DocumentSignedEvent;
import kz.whosnext.hr.auth.model.enums.AuditAction;
import kz.whosnext.hr.auth.model.enums.Role;
import kz.whosnext.hr.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @KafkaListener(topics = "${app.kafka.topics.candidate-promoted}", groupId = "auth-service-group")
    @Transactional
    public void handleCandidatePromoted(String payload) {
        CandidatePromotedEvent event = read(payload, CandidatePromotedEvent.class);
        if (event == null || event.userId() == null || event.newRole() == null) return;
        log.info("Kafka [candidate.promoted]: userId={}, newRole={}", event.userId(), event.newRole());
        userRepository.findById(event.userId()).ifPresent(user -> {
            Role newRole;
            try {
                newRole = Role.valueOf(event.newRole());
            } catch (IllegalArgumentException ex) {
                log.warn("Kafka [candidate.promoted]: unknown role '{}' for userId={}", event.newRole(), event.userId());
                return;
            }
            user.setRole(newRole);
            userRepository.save(user);
            auditService.log(user.getId(), user.getEmail(), AuditAction.ROLE_CHANGED,
                    true, "Роль изменена на " + newRole + " (Kafka: candidate.promoted)");
        });
    }

    @KafkaListener(topics = "${app.kafka.topics.document-signed}", groupId = "auth-service-group")
    @Transactional
    public void handleDocumentSigned(String payload) {
        DocumentSignedEvent event = read(payload, DocumentSignedEvent.class);
        if (event == null) return;
        log.info("Kafka [document.signed]: applicationId={}, candidateId={}", event.applicationId(), event.candidateId());
        // Role promotion (CANDIDATE -> EMPLOYEE) is handled via candidate.promoted event
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

