package kz.whosnext.hr.employeeservice.model.entity;

import jakarta.persistence.*;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceEventType;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceIngestionSource;
import kz.whosnext.hr.employeeservice.model.enums.AttendanceIngestionStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_ingestion_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceIngestionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceIngestionSource source;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AttendanceEventType eventType;

    @Column(length = 500)
    private String location;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false, length = 20)
    private AttendanceIngestionStatus ingestionStatus;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

