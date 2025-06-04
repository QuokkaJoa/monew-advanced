package com.part2.monew.mapper;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import java.sql.Timestamp;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-05T02:33:02+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 17.0.15 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.email( request.email() );
        user.password( request.password() );

        user.active( true );

        return user.build();
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UUID id = null;
        String email = null;

        id = user.getId();
        email = user.getEmail();

        Timestamp createdAt = user.getCreatedAt();
        String nickname = null;

        UserResponse userResponse = new UserResponse( id, email, nickname, createdAt );

        return userResponse;
    }
}
