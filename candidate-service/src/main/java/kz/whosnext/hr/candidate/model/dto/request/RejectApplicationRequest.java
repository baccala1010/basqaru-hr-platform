package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Отклонение заявки")
public record RejectApplicationRequest(String message) {}

