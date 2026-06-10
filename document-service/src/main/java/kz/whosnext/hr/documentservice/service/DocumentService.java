package kz.whosnext.hr.documentservice.service;

import kz.whosnext.hr.documentservice.exception.AccessDeniedException;
import kz.whosnext.hr.documentservice.exception.BadRequestException;
import kz.whosnext.hr.documentservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.documentservice.feign.CandidateFeignClient;
import kz.whosnext.hr.documentservice.mapper.DocumentMapper;
import kz.whosnext.hr.documentservice.model.dto.response.DocumentResponse;
import kz.whosnext.hr.documentservice.model.entity.Document;
import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import kz.whosnext.hr.documentservice.model.enums.ActivityAction;
import kz.whosnext.hr.documentservice.model.enums.ActivitySource;
import kz.whosnext.hr.documentservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final MinioService minioService;
    private final ActivityLogProducer activityLogProducer;
    private final CandidateFeignClient candidateFeignClient;

    private static final String UPLOADS_BUCKET = "hr-uploads";
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Transactional
    public DocumentResponse upload(MultipartFile file, UUID applicationId, UUID candidateId,
                                   UUID vacancyId, String documentType, UUID uploadedBy) {
        if (file.isEmpty()) throw new BadRequestException("Файл пуст");
        if (file.getSize() > MAX_FILE_SIZE) throw new BadRequestException("Файл превышает 10 МБ");
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            throw new BadRequestException("Недопустимый тип файла: " + file.getContentType());

        DocumentType parsedType;
        try {
            parsedType = DocumentType.valueOf(documentType);
        } catch (Exception ex) {
            throw new BadRequestException("Некорректный documentType: " + documentType);
        }

        String prefix = applicationId != null ? "uploads/" + applicationId : "uploads/user-" + uploadedBy;
        String objectKey = minioService.generateObjectKey(prefix, file.getOriginalFilename());
        minioService.uploadFile(UPLOADS_BUCKET, objectKey, file);

        Document doc = Document.builder()
                .applicationId(applicationId)
                .candidateId(candidateId)
                .vacancyId(vacancyId)
                .uploadedBy(uploadedBy)
                .documentType(parsedType)
                .status(DocumentStatus.UPLOADED)
                .fileName(file.getOriginalFilename())
                .minioObjectKey(objectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        doc = documentRepository.save(doc);
        log.info("Документ загружен: id={}, applicationId={}, uploadedBy={}", doc.getId(), applicationId, uploadedBy);

        activityLogProducer.log(
                ActivityAction.DOCUMENT_UPLOADED, ActivitySource.DOCUMENT_SERVICE,
                uploadedBy, null, null,
                "Document", doc.getId().toString(),
                "Document uploaded: " + doc.getFileName());

        return toResponseWithUrl(doc);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(UUID applicationId, UUID candidateId,
                                       String documentType, String role, UUID userId, Pageable pageable) {
        Page<Document> page;
        if ("CANDIDATE".equals(role) || "EMPLOYEE".equals(role)) {
            if (applicationId != null) {
                page = documentRepository.findByApplicationId(applicationId, pageable);
            } else {
                page = documentRepository.findByUploadedBy(userId, pageable);
            }
        } else if (applicationId != null) {
            page = documentRepository.findByApplicationId(applicationId, pageable);
        } else if (documentType != null) {
            page = documentRepository.findByDocumentType(DocumentType.valueOf(documentType), pageable);
        } else {
            page = documentRepository.findAll(pageable);
        }
        return page.map(this::toResponseWithUrl);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id) {
        return toResponseWithUrl(findById(id));
    }

    public InputStream download(UUID id, String role, UUID userId) {
        Document doc = findById(id);
        if ("CANDIDATE".equals(role) || "EMPLOYEE".equals(role)) {
            // Fast path: direct ownership (uploadedBy stores candidate's auth userId for generated contracts)
            if (userId.equals(doc.getUploadedBy()) || userId.equals(doc.getCandidateId())) {
                return minioService.downloadFile(getBucketByStatus(doc.getStatus()), doc.getMinioObjectKey());
            }
            // Fallback for legacy documents where uploadedBy is null:
            // verify ownership via application details from candidate-service
            if (doc.getApplicationId() != null) {
                try {
                    Map<String, Object> appDetails = candidateFeignClient.getApplicationDetails(doc.getApplicationId());
                    Object appUserId = appDetails.get("userId");
                    if (appUserId != null && userId.equals(UUID.fromString(appUserId.toString()))) {
                        return minioService.downloadFile(getBucketByStatus(doc.getStatus()), doc.getMinioObjectKey());
                    }
                } catch (Exception e) {
                    log.warn("Не удалось проверить владельца документа через candidate-service: {}", e.getMessage());
                }
            }
            throw new AccessDeniedException("Нет доступа к этому документу");
        }
        return minioService.downloadFile(getBucketByStatus(doc.getStatus()), doc.getMinioObjectKey());
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        if (!"HR".equals(actorRole) && !"ADMIN".equals(actorRole))
            throw new AccessDeniedException("Удаление доступно только HR и ADMIN");

        Document doc = findById(id);

        activityLogProducer.log(
                ActivityAction.DOCUMENT_DELETED, ActivitySource.DOCUMENT_SERVICE,
                actorId, actorEmail, actorRole,
                "Document", id.toString(),
                "Document deleted: " + doc.getFileName());

        minioService.deleteFile(getBucketByStatus(doc.getStatus()), doc.getMinioObjectKey());
        documentRepository.delete(doc);
        log.info("Документ удалён: id={}", id);
    }

    public Document findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден: " + id));
    }

    public DocumentResponse toResponseWithUrl(Document doc) {
        DocumentResponse base = documentMapper.toResponse(doc);
        String bucket = getBucketByStatus(doc.getStatus());
        String fileUrl = null;
        try {
            fileUrl = minioService.generatePresignedUrl(bucket, doc.getMinioObjectKey());
        } catch (Exception e) {
            log.warn("Не удалось сгенерировать presigned URL для документа {}: {}", doc.getId(), e.getMessage());
        }
        return new DocumentResponse(
                base.id(), base.applicationId(), base.candidateId(), base.vacancyId(),
                base.documentType(), base.status(), base.fileName(), base.contentType(), base.fileSize(),
                fileUrl,
                base.qrCodeUrl(), base.verificationQrUrl(), base.signatureQrUrl(),
                base.signatureHash(), base.signerIin(), base.signedAt(), base.signedBy(),
                base.uploadedBy(),
                base.createdAt(), base.updatedAt()
        );
    }

    private String getBucketByStatus(DocumentStatus status) {
        return switch (status) {
            case UPLOADED -> UPLOADS_BUCKET;
            default -> "hr-documents";
        };
    }
}
