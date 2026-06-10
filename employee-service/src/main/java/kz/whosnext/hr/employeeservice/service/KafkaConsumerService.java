package kz.whosnext.hr.employeeservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.whosnext.hr.employeeservice.event.ApplicationSubmittedEvent;
import kz.whosnext.hr.employeeservice.event.ApplicationUpdatedEvent;
import kz.whosnext.hr.employeeservice.event.CandidatePromotedEvent;
import kz.whosnext.hr.employeeservice.event.DocumentSignedEvent;
import kz.whosnext.hr.employeeservice.feign.CandidateFeignClient;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateEmployeeRequest;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.enums.NotificationType;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final KafkaProducerService kafkaProducerService;
    private final CandidateFeignClient candidateFeignClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.document-signed}", groupId = "employee-service-group")
    @Transactional
    public void handleDocumentSigned(String payload) {
        DocumentSignedEvent event = read(payload, DocumentSignedEvent.class);
        if (event == null) {
            return;
        }
        log.info("Kafka [document.signed]: applicationId={}, candidateId={}", event.applicationId(), event.candidateId());

        if (event.candidateId() == null) return;

        // Уведомляем HR о подписании документов
        notifyHrEmployees(
                "Кандидат подписал документы",
                "Документы подписаны, кандидат готов к переводу. applicationId=" + event.applicationId(),
                NotificationType.DOCUMENT_SIGNED,
                event.applicationId(),
                "APPLICATION"
        );

        UUID actualUserId;
        String firstName;
        String lastName;
        String email;
        String positionName;
        try {
            Map<String, Object> details = candidateFeignClient.getApplicationDetails(event.applicationId());
            actualUserId = details.get("userId") != null ? UUID.fromString(details.get("userId").toString()) : null;
            firstName = details.get("firstName") != null ? details.get("firstName").toString() : null;
            lastName = details.get("lastName") != null ? details.get("lastName").toString() : null;
            email = details.get("email") != null ? details.get("email").toString() : null;
            positionName = details.get("positionName") != null ? details.get("positionName").toString() : null;
        } catch (Exception e) {
            log.warn("Не удалось получить данные заявки из candidate-service: {}", e.getMessage());
            actualUserId = null;
            firstName = null;
            lastName = null;
            email = null;
            positionName = null;
        }

        final UUID resolvedUserId = actualUserId != null ? actualUserId : event.candidateId();
        final String resolvedEmail = email != null ? email : "noreply@hr.local";
        final String resolvedFirstName = firstName != null ? firstName : "—";
        final String resolvedLastName = lastName != null ? lastName : "—";
        final String resolvedPositionName = positionName != null ? positionName : "Не указана";

        employeeRepository.findAll().stream()
                .filter(e -> event.candidateId().equals(e.getCandidateId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            log.info("Сотрудник уже существует для candidateId={}", event.candidateId());
                            notificationService.createByEmployeeId(existing.getId(),
                                    "Документы подписаны",
                                    "Все документы успешно подписаны",
                                    NotificationType.DOCUMENT_SIGNED, event.applicationId(), "APPLICATION");
                        },
                        () -> {
                            log.info("Создание сотрудника из candidateId={}", event.candidateId());
                            try {
                                var emp = employeeService.create(new CreateEmployeeRequest(
                                        resolvedUserId,
                                        event.candidateId(),
                                        event.applicationId(),
                                        null, null,
                                        resolvedPositionName, null,
                                        resolvedFirstName,
                                        resolvedLastName, null, null,
                                        resolvedEmail,
                                        null, null, null,
                                        LocalDate.now()),
                                        resolvedUserId, resolvedEmail, "SYSTEM");

                                kafkaProducerService.sendCandidatePromoted(
                                        new CandidatePromotedEvent(resolvedUserId, "EMPLOYEE"));
                                log.info("Сотрудник создан и роль изменена: id={}, userId={}", emp.id(), resolvedUserId);
                            } catch (Exception e) {
                                log.error("Ошибка создания сотрудника: {}", e.getMessage());
                            }
                        }
                );
    }

    @KafkaListener(topics = "${app.kafka.topics.application-submitted}", groupId = "employee-service-group")
    @Transactional
    public void handleApplicationSubmitted(String payload) {
        ApplicationSubmittedEvent event = read(payload, ApplicationSubmittedEvent.class);
        if (event == null || event.applicationId() == null) {
            return;
        }
        log.info("Kafka [application.submitted]: applicationId={}, candidateEmail={}, vacancyTitle={}",
                event.applicationId(), event.candidateEmail(), event.vacancyTitle());

        notifyHrEmployees(
                "Новая заявка от кандидата",
                event.candidateEmail() + " подал(а) заявку на вакансию «" + event.vacancyTitle() + "»",
                NotificationType.GENERAL,
                event.applicationId(),
                "APPLICATION"
        );
    }

    @KafkaListener(topics = "${app.kafka.topics.application-updated}", groupId = "employee-service-group")
    @Transactional
    public void handleApplicationUpdated(String payload) {
        ApplicationUpdatedEvent event = read(payload, ApplicationUpdatedEvent.class);
        if (event == null || event.applicationId() == null) {
            return;
        }
        log.info("Kafka [application.updated]: applicationId={}, candidateEmail={}",
                event.applicationId(), event.candidateEmail());

        notifyHrEmployees(
                "Кандидат обновил заявку",
                "Заявка обновлена после доработки: applicationId=" + event.applicationId()
                        + ", email=" + event.candidateEmail(),
                NotificationType.GENERAL,
                event.applicationId(),
                "APPLICATION"
        );
    }

    private void notifyHrEmployees(String title, String message, NotificationType type,
                                    UUID refId, String refType) {
        List<Employee> targets = employeeRepository.findAll().stream()
                .filter(e -> {
                    String pos = e.getPositionName() != null ? e.getPositionName().toLowerCase() : "";
                    return pos.contains("hr") || pos.contains("admin") || pos.contains("director");
                })
                .toList();

        if (targets.isEmpty()) {
            log.warn("HR/ADMIN/DIRECTOR employees not found for notification: title={}", title);
            return;
        }

        targets.forEach(emp -> notificationService.createByEmployeeId(
                emp.getId(), title, message, type, refId, refType));
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

