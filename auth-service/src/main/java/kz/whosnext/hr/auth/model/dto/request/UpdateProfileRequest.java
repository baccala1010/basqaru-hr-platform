package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление профиля")
public record UpdateProfileRequest(

        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁіІңҢғҒүҮұҰқҚөӨһҺ]+$",
                message = "Имя должно содержать только буквы")
        @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
        @Schema(description = "Имя", example = "Бахтияр")
        String firstName,

        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁіІңҢғҒүҮұҰқҚөӨһҺ]+$",
                message = "Фамилия должна содержать только буквы")
        @Size(min = 2, max = 100, message = "Фамилия должна быть от 2 до 100 символов")
        @Schema(description = "Фамилия", example = "Байзулла")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный номер телефона")
        @Schema(description = "Телефон", example = "+77001234567")
        String phone,

        @Size(max = 1000, message = "Ссылка на фото не должна превышать 1000 символов")
        @Schema(description = "Ссылка на фото профиля", example = "https://cdn.example.kz/users/u-123/avatar.jpg")
        String photoUrl
) {
}

