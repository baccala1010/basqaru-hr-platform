package kz.whosnext.hr.candidate.model.dto.response;

import java.util.UUID;

public record DictionaryResponse(
        UUID id,
        String name
) {}

