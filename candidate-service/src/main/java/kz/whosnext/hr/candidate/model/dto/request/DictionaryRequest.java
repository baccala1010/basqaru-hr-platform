package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Создание / обновление должности или типа договора")
public record DictionaryRequest(
        @NotBlank(message = "Название обязательно")
        String name
) {}

