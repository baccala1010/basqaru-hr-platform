package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        String employeeFullName,
        LocalDate date,
        boolean present,
        LocalTime checkIn,
        LocalTime checkOut,
        String location,
        LocalDateTime createdAt
) {}

