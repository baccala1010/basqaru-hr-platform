package kz.whosnext.hr.employeeservice.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID employeeId,
        String employeeFullName,
        LocalDate date,
        LocalTime checkIn,
        LocalTime checkOut,
        BigDecimal hoursWorked,
        String status,
        String note,
        LocalDateTime createdAt
) {}

