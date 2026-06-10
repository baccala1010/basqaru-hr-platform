package kz.whosnext.hr.auth.mapper;

import kz.whosnext.hr.auth.model.dto.response.UserResponse;
import kz.whosnext.hr.auth.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserResponse toResponse(User user);
}

