package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PositionResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt
) {}

