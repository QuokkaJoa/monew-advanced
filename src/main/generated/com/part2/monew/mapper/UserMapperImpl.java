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
    date = "2025-06-03T16:41:55+0900",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.jar, environment: Java 17.0.6 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        String email = null;
        String password = null;

        email = request.email();
        password = request.password();

        boolean active = true;
        Timestamp createdAt = null;
        String username = null;

        User user = new User( username, email, password, active, createdAt );

        return user;
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
