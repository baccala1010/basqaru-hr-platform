package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String description,
        UUID parentId,
        UUID managerId,
        long headCount,
        LocalDateTime createdAt
) {}

