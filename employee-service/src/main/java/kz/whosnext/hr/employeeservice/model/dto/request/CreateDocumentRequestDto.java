package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequestDto(
        @NotBlank
        String documentType,
        String comment
) {}

