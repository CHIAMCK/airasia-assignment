package com.example.shortener.service;

import com.example.shortener.entity.User;
import com.example.shortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("returns saved user when email is new")
        void returnsSavedUserWhenEmailIsNew() {
            User user = new User("Alice", "alice@example.com");
            User savedUser = new User("Alice", "alice@example.com");
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.createUser("Alice", "alice@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Alice");
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws when email already exists")
        void throwsWhenEmailAlreadyExists() {
            User existingUser = new User("Bob", "bob@example.com");
            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> userService.createUser("Bob", "bob@example.com"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User with email already exists: bob@example.com");

            verify(userRepository).findByEmail("bob@example.com");
        }

        @Test
        @DisplayName("does not call save when email exists")
        void doesNotCallSaveWhenEmailExists() {
            when(userRepository.findByEmail("existing@example.com")).thenReturn(
                    Optional.of(new User("Existing", "existing@example.com")));

            assertThatThrownBy(() -> userService.createUser("New", "existing@example.com"))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository).findByEmail("existing@example.com");
        }
    }
}
