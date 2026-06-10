package kz.whosnext.hr.documentservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SignDocumentRequest(
        @NotNull(message = "Необходимо подтверждение")
        Boolean confirmed,
        String ipAddress
) {}

