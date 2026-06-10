package kz.whosnext.hr.candidate.model.dto.response;

import kz.whosnext.hr.candidate.model.enums.ApplicationStatus;
import kz.whosnext.hr.candidate.model.enums.TimelineStepCode;
import kz.whosnext.hr.candidate.model.enums.TimelineStepStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationTimelineResponse(
        UUID applicationId,
        ApplicationStatus applicationStatus,
        List<Step> steps
) {
    public record Step(
            TimelineStepCode code,
            String title,
            TimelineStepStatus status,
            String message,
            LocalDateTime occurredAt
    ) {
    }
}

