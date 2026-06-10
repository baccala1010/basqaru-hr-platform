package kz.whosnext.hr.auth.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Kafka-событие: пользователь зарегистрирован")
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName
) {
}

