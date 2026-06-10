package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayCalendarResponse(
        UUID id,
        LocalDate holidayDate,
        String name,
        boolean paid
) {
}

