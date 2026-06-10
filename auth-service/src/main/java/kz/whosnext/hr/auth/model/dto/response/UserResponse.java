package kz.whosnext.hr.auth.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.whosnext.hr.auth.model.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Профиль пользователя")
public record UserResponse(

        @Schema(description = "ID пользователя")
        UUID id,

        @Schema(description = "Email")
        String email,

        @Schema(description = "Имя")
        String firstName,

        @Schema(description = "Фамилия")
        String lastName,

        @Schema(description = "Телефон")
        String phone,

        @Schema(description = "Ссылка на фото профиля")
        String photoUrl,

        @Schema(description = "Роль")
        Role role,

        @Schema(description = "Email подтверждён")
        Boolean emailVerified,

        @Schema(description = "2FA включена")
        Boolean twoFactorEnabled,

        @Schema(description = "Согласие на обработку персональных данных")
        Boolean personalDataConsent,

        @Schema(description = "Дата регистрации")
        LocalDateTime createdAt
) {
}

