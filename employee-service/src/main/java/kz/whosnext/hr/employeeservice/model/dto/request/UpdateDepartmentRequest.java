package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateDepartmentRequest(
        @Size(max = 200)
        String name,
        String description,
        UUID parentId,
        UUID managerId
) {}

