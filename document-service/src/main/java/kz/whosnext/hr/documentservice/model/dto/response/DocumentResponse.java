package kz.whosnext.hr.documentservice.model.dto.response;

import kz.whosnext.hr.documentservice.model.enums.DocumentStatus;
import kz.whosnext.hr.documentservice.model.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID applicationId,
        UUID candidateId,
        UUID vacancyId,
        DocumentType documentType,
        DocumentStatus status,
        String fileName,
        String contentType,
        Long fileSize,
        String fileUrl,
        String qrCodeUrl,
        String verificationQrUrl,
        String signatureQrUrl,
        String signatureHash,
        String signerIin,
        LocalDateTime signedAt,
        UUID signedBy,
        UUID uploadedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

