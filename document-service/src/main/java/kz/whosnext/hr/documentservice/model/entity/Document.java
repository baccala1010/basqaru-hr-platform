package kz.whosnext.hr.documentservice.model.entity;

import jakarta.persistence.*;
import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "candidate_id")
    private UUID candidateId;

    @Column(name = "vacancy_id")
    private UUID vacancyId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.GENERATED;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "minio_object_key", nullable = false, length = 1000)
    private String minioObjectKey;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signed_by")
    private UUID signedBy;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "qr_code_url", length = 1000)
    private String qrCodeUrl;

    @Column(name = "verification_qr_url", length = 1000)
    private String verificationQrUrl;

    @Column(name = "signature_qr_url", length = 1000)
    private String signatureQrUrl;

    @Column(name = "signature_hash", length = 128)
    private String signatureHash;

    @Column(name = "signer_iin", length = 12)
    private String signerIin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

