package com.mingle.a2srv_service.mapper;

import com.mingle.a2srv_service.dto.AuthResponse;
import com.mingle.a2srv_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "token", source = "token")
    @Mapping(target = "userId", expression = "java(user.getId().toString())")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "verified", source = "user.verified")
    AuthResponse toResponse(User user, String token);
}
