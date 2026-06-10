package kz.whosnext.hr.auth.event;

import java.util.UUID;

public record CandidatePromotedEvent(
        UUID userId,
        String newRole
) {
}

