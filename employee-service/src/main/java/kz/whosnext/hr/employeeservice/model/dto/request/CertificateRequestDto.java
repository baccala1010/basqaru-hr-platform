package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CertificateRequestDto(
        @NotNull UUID employeeId,
        @NotNull String certificateType
) {}

