package kz.whosnext.hr.documentservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EcpSignRequest(
        @NotBlank(message = "Подписанный XML обязателен")
        String signedXml
) {}
