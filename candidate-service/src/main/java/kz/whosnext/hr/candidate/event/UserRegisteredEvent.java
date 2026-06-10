package kz.whosnext.hr.candidate.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName
) {}

