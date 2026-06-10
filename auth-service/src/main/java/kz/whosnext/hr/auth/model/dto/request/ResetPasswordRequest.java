package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Запрос на сброс пароля — установка нового")
public record ResetPasswordRequest(

        @NotBlank(message = "Токен обязателен")
        @Schema(description = "Токен сброса пароля из письма")
        String token,

        @NotBlank(message = "Новый пароль обязателен")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$]).{8,}$",
                message = "Пароль: мин. 8 символов, 1 строчная, 1 заглавная, 1 цифра, 1 спецсимвол (!@#$)")
        @Schema(description = "Новый пароль", example = "NewPassword1!")
        String newPassword
) {
}

