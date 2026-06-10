package kz.whosnext.hr.candidate.model.entity;

import jakarta.persistence.*;
import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.model.enums.InterviewType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private Vacancy vacancy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 12)
    private String iin;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    @Column(name = "personal_data_consent", nullable = false)
    private Boolean personalDataConsent;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", length = 20)
    private InterviewType interviewType;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(name = "interview_message", columnDefinition = "TEXT")
    private String interviewMessage;

    @Column(name = "rejection_message", columnDefinition = "TEXT")
    private String rejectionMessage;

    @Column(name = "revision_message", columnDefinition = "TEXT")
    private String revisionMessage;

    @Column(name = "revision_comment", columnDefinition = "TEXT")
    private String revisionComment;

    @Column(name = "documents_ready", nullable = false)
    @Builder.Default
    private Boolean documentsReady = false;

    @Column(name = "documents_signed", nullable = false)
    @Builder.Default
    private Boolean documentsSigned = false;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "promoted_by")
    private UUID promotedBy;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApplicationDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
