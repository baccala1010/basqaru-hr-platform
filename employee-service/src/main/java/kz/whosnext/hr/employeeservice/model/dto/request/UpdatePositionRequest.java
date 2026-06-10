package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePositionRequest(
        @Size(max = 200)
        String name,
        String description
) {}

