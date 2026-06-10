package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Обновление профиля кандидата")
public record UpdateCandidateRequest(
        @Size(min = 2, max = 100)
        String firstName,
        @Size(min = 2, max = 100)
        String lastName,
        LocalDate birthDate,
        @Pattern(regexp = "^\\d{12}$", message = "ИИН должен содержать ровно 12 цифр")
        String iin,
        @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Некорректный номер телефона")
        String phone,
        String address,
        String activityType
) {}

