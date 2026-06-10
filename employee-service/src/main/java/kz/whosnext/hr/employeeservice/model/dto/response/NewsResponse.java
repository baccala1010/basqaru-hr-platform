package kz.whosnext.hr.employeeservice.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NewsResponse(
        UUID id,
        String title,
        String content,
        String imageUrl,
        UUID authorId,
        boolean published,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {}

