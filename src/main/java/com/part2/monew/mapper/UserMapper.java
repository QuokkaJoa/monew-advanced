package com.part2.monew.mapper;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt())")
    UserResponse toResponse(User user);

}
