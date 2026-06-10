package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateLeaveStatusRequest(
        @NotBlank String status,
        String rejectReason
) {}

