package com.part2.monew.repository;

import com.part2.monew.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User saved;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("tester");
        user.setPassword("123456");
        user.setActive(true);
        saved = userRepository.save(user);
    }

    @Test
    void testExistsByEmail() {
        // when
        boolean exists = userRepository.existsByEmail("test@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void testfindByIdAndActiveTrue() {

        // when
        Optional<User> result = userRepository.findByIdAndActiveTrue(saved.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }


    @Test
    void testFindByEmailAndActiveTrue() {
        // when
        Optional<User> result = userRepository.findByEmailAndActiveTrue("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("tester");
    }

}
