package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Запрос на регистрацию пользователя")
public record RegisterRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Некорректный формат email")
        @Schema(description = "Email пользователя", example = "user@example.com")
        String email,

        @NotBlank(message = "Пароль обязателен")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$]).{8,}$",
                message = "Пароль: мин. 8 символов, 1 строчная, 1 заглавная, 1 цифра, 1 спецсимвол (!@#$)")
        @Schema(description = "Пароль", example = "Password1!")
        String password,

        @NotBlank(message = "Имя обязательно")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁіІңҢғҒүҮұҰқҚөӨһҺ]+$",
                message = "Имя должно содержать только буквы")
        @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
        @Schema(description = "Имя", example = "Бахтияр")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁіІңҢғҒүҮұҰқҚөӨһҺ]+$",
                message = "Фамилия должна содержать только буквы")
        @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
        @Schema(description = "Фамилия", example = "Байзулла")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный номер телефона")
        @Schema(description = "Телефон", example = "+77001234567")
        String phone,

        @Schema(description = "Согласие на пуш-уведомления", example = "true")
        Boolean pushNotificationConsent,

        @NotNull(message = "Необходимо согласие на обработку персональных данных")
        @AssertTrue(message = "Необходимо согласие на обработку персональных данных")
        @Schema(description = "Согласие на обработку персональных данных", example = "true")
        Boolean personalDataConsent
) {
}
