package com.part2.monew.service;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.request.UserUpdateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.user.EmailDuplicateException;
import com.part2.monew.global.exception.user.NoPermissionToDeleteException;
import com.part2.monew.global.exception.user.NoPermissionToUpdateException;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.mapper.UserMapper;
import com.part2.monew.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse createUser(UserCreateRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new EmailDuplicateException("이미 사용 중인 이메일입니다.");
        }

        User user = userMapper.toEntity(request);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public UserResponse updateNickname(UUID userId, UUID requestUserId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToUpdateException("사용자 수정 권한이 없습니다.");
        }
        user.setNickname(request.getNickname());
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public void delete(UUID userId, UUID requestUserId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToDeleteException("사용자 삭제 권한이 없습니다.");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    public void deleteHard(UUID userId, UUID requestUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("해당 사용자를 찾을 수 없습니다."));

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToDeleteException("사용자 삭제 권한이 없습니다.");
        }

        userRepository.delete(user);
    }
}
