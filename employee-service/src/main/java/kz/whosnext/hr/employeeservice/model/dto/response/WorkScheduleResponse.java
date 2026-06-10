package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record WorkScheduleResponse(
        UUID id,
        String name,
        String type,
        LocalTime startTime,
        LocalTime endTime,
        String workingDays,
        LocalDateTime createdAt
) {}

