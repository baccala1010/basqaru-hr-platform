package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDocumentRequestStatusDto(
        @NotBlank
        String status,
        String fileUrl,
        String rejectReason
) {}

