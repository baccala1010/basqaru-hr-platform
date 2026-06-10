package kz.whosnext.hr.auth.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Ответ с ошибкой")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        @Schema(description = "HTTP статус код", example = "400")
        int status,

        @Schema(description = "Краткое описание ошибки", example = "Bad Request")
        String error,

        @Schema(description = "Детальное сообщение")
        String message,

        @Schema(description = "Путь запроса", example = "/api/v1/auth/register")
        String path,

        @Schema(description = "Время ошибки")
        LocalDateTime timestamp,

        @Schema(description = "Детали валидации (поле → ошибка)")
        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, LocalDateTime.now(), null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(status, error, message, path, LocalDateTime.now(), validationErrors);
    }
}

