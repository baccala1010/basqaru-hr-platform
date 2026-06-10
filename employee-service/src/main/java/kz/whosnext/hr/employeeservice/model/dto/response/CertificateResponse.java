package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID employeeId,
        String employeeFullName,
        String certificateType,
        String status,
        UUID generatedDocumentId,
        String rejectReason,
        LocalDateTime createdAt
) {}

