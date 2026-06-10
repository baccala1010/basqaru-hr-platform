package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record CandidatePromotedEvent(
        UUID userId,
        String newRole
) {}

