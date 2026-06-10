package kz.whosnext.hr.employeeservice.event;

import java.util.UUID;

public record CandidatePromotedEvent(
        UUID userId,
        String newRole
) {}

