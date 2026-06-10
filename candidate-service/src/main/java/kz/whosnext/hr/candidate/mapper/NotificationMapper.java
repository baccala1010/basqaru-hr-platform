package kz.whosnext.hr.candidate.mapper;

import kz.whosnext.hr.candidate.model.dto.response.NotificationResponse;
import kz.whosnext.hr.candidate.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}

