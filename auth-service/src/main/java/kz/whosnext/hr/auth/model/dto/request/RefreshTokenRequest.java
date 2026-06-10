package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на обновление токенов")
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token обязателен")
        @Schema(description = "Refresh token")
        String refreshToken
) {
}
