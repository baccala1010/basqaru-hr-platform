package kz.whosnext.hr.auth.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kz.whosnext.hr.auth.model.enums.Role;

@Schema(description = "Запрос на смену роли пользователя")
public record ChangeRoleRequest(

        @NotNull(message = "Роль обязательна")
        @Schema(description = "Новая роль", example = "EMPLOYEE")
        Role role
) {
}

