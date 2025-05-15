package com.mingle.a2srv_service.mapper;

import com.mingle.a2srv_service.dto.UserRequest;
import com.mingle.a2srv_service.dto.UserResponse;
import com.mingle.a2srv_service.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRequest request);

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User user, UserRequest request);
}
