package kz.whosnext.hr.documentservice.service;

import kz.whosnext.hr.documentservice.exception.AccessDeniedException;
import kz.whosnext.hr.documentservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.documentservice.mapper.DocumentMapper;
import kz.whosnext.hr.documentservice.model.dto.response.TemplateResponse;
import kz.whosnext.hr.documentservice.model.entity.DocumentTemplate;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;
import kz.whosnext.hr.documentservice.model.enums.ActivityAction;
import kz.whosnext.hr.documentservice.model.enums.ActivitySource;
import kz.whosnext.hr.documentservice.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final DocumentTemplateRepository templateRepository;
    private final DocumentMapper documentMapper;
    private final MinioService minioService;
    private final ActivityLogProducer activityLogProducer;

    private static final String TEMPLATES_BUCKET = "hr-templates";

    @Transactional(readOnly = true)
    public List<TemplateResponse> listAll() {
        return templateRepository.findByActiveTrue().stream()
                .map(documentMapper::toTemplateResponse)
                .toList();
    }

    @Transactional
    public TemplateResponse upload(MultipartFile file, String name, String documentType,
                                   String description, UUID userId, String role) {
        if (!"ADMIN".equals(role)) throw new AccessDeniedException("Загрузка шаблонов доступна только ADMIN");

        String objectKey = minioService.generateObjectKey("templates", file.getOriginalFilename());
        minioService.uploadFile(TEMPLATES_BUCKET, objectKey, file);

        DocumentTemplate template = DocumentTemplate.builder()
                .name(name)
                .documentType(DocumentType.valueOf(documentType))
                .description(description)
                .fileName(file.getOriginalFilename())
                .minioObjectKey(objectKey)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .createdBy(userId)
                .build();

        template = templateRepository.save(template);
        log.info("Шаблон загружен: id={}, type={}", template.getId(), documentType);

        activityLogProducer.log(
                ActivityAction.TEMPLATE_UPLOADED, ActivitySource.DOCUMENT_SERVICE,
                userId, null, role,
                "DocumentTemplate", template.getId().toString(),
                "Template uploaded: " + name);

        return documentMapper.toTemplateResponse(template);
    }

    @Transactional
    public TemplateResponse update(UUID id, MultipartFile file, String name, String description, String role) {
        if (!"ADMIN".equals(role) && !"HR".equals(role))
            throw new AccessDeniedException("Обновление шаблонов доступно только ADMIN/HR");

        DocumentTemplate template = findById(id);

        if (file != null && !file.isEmpty()) {
            minioService.deleteFile(TEMPLATES_BUCKET, template.getMinioObjectKey());
            String objectKey = minioService.generateObjectKey("templates", file.getOriginalFilename());
            minioService.uploadFile(TEMPLATES_BUCKET, objectKey, file);
            template.setMinioObjectKey(objectKey);
            template.setFileName(file.getOriginalFilename());
            template.setContentType(file.getContentType());
            template.setFileSize(file.getSize());
        }

        if (name != null) template.setName(name);
        if (description != null) template.setDescription(description);

        template = templateRepository.save(template);
        log.info("Шаблон обновлён: id={}", id);

        activityLogProducer.log(
                ActivityAction.TEMPLATE_UPDATED, ActivitySource.DOCUMENT_SERVICE,
                null, null, role,
                "DocumentTemplate", id.toString(),
                "Template updated: " + template.getName());

        return documentMapper.toTemplateResponse(template);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        if (!"ADMIN".equals(actorRole)) throw new AccessDeniedException("Удаление шаблонов доступно только ADMIN");

        DocumentTemplate template = findById(id);

        activityLogProducer.log(
                ActivityAction.TEMPLATE_DELETED, ActivitySource.DOCUMENT_SERVICE,
                actorId, actorEmail, actorRole,
                "DocumentTemplate", id.toString(),
                "Template deleted: " + template.getName());

        minioService.deleteFile(TEMPLATES_BUCKET, template.getMinioObjectKey());
        templateRepository.delete(template);
        log.info("Шаблон удалён: id={}", id);
    }

    public DocumentTemplate findById(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон не найден: " + id));
    }

    public DocumentTemplate findActiveByType(DocumentType type) {
        return templateRepository.findByDocumentTypeAndActiveTrue(type)
                .orElseThrow(() -> new ResourceNotFoundException("Активный шаблон не найден для типа: " + type));
    }
}

