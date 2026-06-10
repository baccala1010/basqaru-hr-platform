package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentRequestResponse(
        UUID id,
        UUID employeeId,
        String employeeFullName,
        String documentType,
        String comment,
        String status,
        String fileUrl,
        String rejectReason,
        UUID signedBy,
        LocalDateTime signedAt,
        LocalDateTime createdAt
) {}

