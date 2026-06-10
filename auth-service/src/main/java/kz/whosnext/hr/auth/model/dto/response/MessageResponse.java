package kz.whosnext.hr.auth.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Текстовое сообщение")
public record MessageResponse(

        @Schema(description = "Сообщение")
        String message
) {
}

