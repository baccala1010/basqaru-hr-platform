package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на сброс пароля — отправка письма")
public record ForgotPasswordRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        @Schema(description = "Email пользователя", example = "user@example.com")
        String email
) {
}

