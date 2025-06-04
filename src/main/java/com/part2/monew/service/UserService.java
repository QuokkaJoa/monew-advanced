package com.part2.monew.service;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.request.UserLoginRequest;
import com.part2.monew.dto.request.UserUpdateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    User loginUser(UserLoginRequest request);

    UserResponse updateNickname(UUID userId, UUID requestUserId, UserUpdateRequest request);

    void delete(UUID userId, UUID requestUserId);

    void deleteHard(UUID userId, UUID requestUserId);
}
