package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.event.CandidatePromotedEvent;
import kz.whosnext.hr.employeeservice.event.AttendanceIngestedEvent;
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

    @Value("${app.kafka.topics.candidate-promoted}")
    private String candidatePromotedTopic;

    @Value("${app.kafka.topics.attendance-ingested}")
    private String attendanceIngestedTopic;

    public void sendCandidatePromoted(CandidatePromotedEvent event) {
        kafkaTemplate.send(candidatePromotedTopic, event.userId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka [{}] ошибка: {}", candidatePromotedTopic, ex.getMessage());
                    else log.info("Kafka [{}] отправлено: userId={}", candidatePromotedTopic, event.userId());
                });
    }

    public void sendAttendanceIngested(AttendanceIngestedEvent event) {
        kafkaTemplate.send(attendanceIngestedTopic, event.employeeId().toString(), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) log.error("Kafka [{}] ошибка: {}", attendanceIngestedTopic, ex.getMessage());
                    else log.info("Kafka [{}] отправлено: employeeId={}, ingestionEventId={}",
                            attendanceIngestedTopic, event.employeeId(), event.ingestionEventId());
                });
    }
}

