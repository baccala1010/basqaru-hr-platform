package kz.whosnext.hr.documentservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateCertificateRequest(
        @NotNull(message = "employeeId обязателен")
        UUID employeeId,
        @NotBlank(message = "certificateType обязателен")
        String certificateType,
        String employeeFullName,
        String position,
        String department,
        String hireDate,
        String additionalInfo
) {}
