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
    date = "2025-06-05T13:35:00+0900",
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

        user.username( request.nickname() );
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
        String username = null;

        id = user.getId();
        email = user.getEmail();
        username = user.getUsername();

        Timestamp createdAt = user.getCreatedAt();

        UserResponse userResponse = new UserResponse( id, email, username, createdAt );

        return userResponse;
    }
}
