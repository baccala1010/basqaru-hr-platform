package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Одобрение заявки")
public record ApproveApplicationRequest(
        String message
) {}
