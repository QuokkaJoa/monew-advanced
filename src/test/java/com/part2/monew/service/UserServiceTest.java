package com.part2.monew.service;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.request.UserLoginRequest;
import com.part2.monew.dto.request.UserUpdateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import com.part2.monew.mapper.UserMapper;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserCreateRequest createReq;
    private UserUpdateRequest updateReq;
    private UserLoginRequest loginReq;
    private User user;
    private UserResponse response;
    private UUID requestUserId;

    @BeforeEach
    public void setUp() {
        createReq = new UserCreateRequest("Woody@naver.com", "woody","123456");
        updateReq = new UserUpdateRequest("updatedWoody");
        loginReq = new UserLoginRequest("Woody@naver.com", "123456");
        user = User.builder()
                .id(UUID.randomUUID())
                .email("woody@naver.com")
                .username("woody")
                .password("123456")
                .active(true).build();
        requestUserId = user.getId();
    }

    @Test
    public void testCreateUser() {
        // given
        given(userRepository.existsByEmail(createReq.email())).willReturn(false);
        given(userMapper.toEntity(createReq)).willReturn(user);
        given(userMapper.toResponse(user)).willReturn(response);

        // when
        UserResponse userResponse = userService.createUser(createReq);

        // then
        assertThat(userResponse).isEqualTo(response);
    }

    @Test
    public void testLoginUser() {
        // given
        given(userRepository.findByEmailAndActiveTrue(loginReq.email())).willReturn(Optional.of(user));

        // when
        User result = userService.loginUser(loginReq);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    public void testUpdateUser() {
        // given
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(userMapper.toResponse(user)).willReturn(response);

        // when
        UserResponse result = userService.updateNickname(user.getId(),requestUserId,updateReq);

        // then
        assertThat(result.nickname()).isEqualTo(updateReq.getNickname());
    }

    @Test
    public void testDeleteUser() {
        // given
        user.setActive(true);
        given(userRepository.findByIdAndActiveTrue(user.getId()))
                .willReturn(Optional.of(user));

        // when
        userService.delete(user.getId(),requestUserId);

        // then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    public void testHardDeleteUser() {
        // given
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        // when
        userService.deleteHard(user.getId(),requestUserId);

        // then
        then(userRepository).should().delete(user);
    }

}
