package kz.whosnext.hr.employeeservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NewsRequest(
        @NotBlank
        String title,
        @NotBlank
        String content,
        String imageUrl,
        boolean published
) {}

