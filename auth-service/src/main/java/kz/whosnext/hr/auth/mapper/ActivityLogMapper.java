package kz.whosnext.hr.auth.mapper;

import kz.whosnext.hr.auth.model.dto.response.ActivityLogResponse;
import kz.whosnext.hr.auth.model.entity.ActivityLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActivityLogMapper {

    public static ActivityLogResponse toResponse(ActivityLog entity) {
        return new ActivityLogResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getActorEmail(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getSource(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}
