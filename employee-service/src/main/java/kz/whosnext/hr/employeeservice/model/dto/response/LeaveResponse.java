package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeaveResponse(
        UUID id,
        UUID employeeId,
        String employeeFullName,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int daysCount,
        String status,
        String reason,
        String rejectReason,
        UUID approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {}

