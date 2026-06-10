package kz.whosnext.hr.documentservice.model.entity;

import jakarta.persistence.*;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "minio_object_key", nullable = false, length = 1000)
    private String minioObjectKey;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

