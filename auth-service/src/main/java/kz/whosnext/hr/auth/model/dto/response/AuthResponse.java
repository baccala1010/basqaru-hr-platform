package kz.whosnext.hr.auth.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с JWT-токенами")
public record AuthResponse(

        @Schema(description = "Access token (15 мин)")
        String accessToken,

        @Schema(description = "Refresh token (7 дней)")
        String refreshToken,

        @Schema(description = "Тип токена", example = "Bearer")
        String tokenType
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}

