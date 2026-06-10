package kz.whosnext.hr.documentservice.model.dto.response;

import kz.whosnext.hr.documentservice.model.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        DocumentType documentType,
        String description,
        String fileName,
        String contentType,
        Long fileSize,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

