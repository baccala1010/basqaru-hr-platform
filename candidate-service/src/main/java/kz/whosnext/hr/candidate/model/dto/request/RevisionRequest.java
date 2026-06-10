package kz.whosnext.hr.candidate.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос доработки заявки")
public record RevisionRequest(
        String documentToUpload,
        String documentToRedo,
        String documentToAttach,
        String comment
) {}

