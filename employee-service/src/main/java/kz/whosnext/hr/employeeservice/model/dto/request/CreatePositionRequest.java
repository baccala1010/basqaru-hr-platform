package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank
        @Size(max = 200)
        String name,
        String description
) {}

