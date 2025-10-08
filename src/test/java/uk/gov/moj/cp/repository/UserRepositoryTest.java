package uk.gov.moj.cp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.model.User;
import uk.gov.moj.cp.service.UserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setEmail("alice@example.com");
        user.setRole("ADMIN");
        user.setActive(true);
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSaveUser() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = userService.createUser(user);

        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should fetch user by email successfully")
    void testFindByEmailSuccess() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        User found = userService.getUser("alice@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository, times(1)).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void testFindByEmailNotFound() {
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("bob@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with email: bob@example.com");

        verify(userRepository, times(1)).findByEmail("bob@example.com");
    }

    @Test
    @DisplayName("Should return empty when user is not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        Optional<User> retrievedUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(retrievedUser).isEmpty();
    }
}
