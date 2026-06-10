package kz.whosnext.hr.candidate.model.dto.response;

import kz.whosnext.hr.candidate.model.enums.CandidateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String iin,
        String phone,
        String address,
        String activityType,
        CandidateStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

