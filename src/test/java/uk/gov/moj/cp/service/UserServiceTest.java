package uk.gov.moj.cp.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.ActiveStatus;
import uk.gov.moj.cp.model.Roles;
import uk.gov.moj.cp.model.User;
import uk.gov.moj.cp.repository.UserRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Roles.ADMIN.name());
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.createUser(user);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid role")
    void testCreateUser_InvalidRole() {
        User user = new User();
        user.setEmail("invalid@example.com");
        user.setRole("INVALID_ROLE"); // invalid
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        assertThatThrownBy(() -> userService.createUser(user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid role");
    }

    @Test
    @DisplayName("Should update existing user")
    void testUpdateUser_Success() {
        final UUID id = UUID.randomUUID();
        User existingUser = new User();
        //existingUser.setId(id);
        existingUser.setEmail("old@example.com");
        existingUser.setRole(Roles.USER.name());
        existingUser.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        User updatedUser = new User();
        updatedUser.setEmail("new@example.com");
        updatedUser.setRole(Roles.ADMIN.name());
        updatedUser.setActive(Boolean.valueOf(ActiveStatus.FALSE.name()));

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = userService.updateUser(id, updatedUser);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(result.isActive()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void testUpdateUser_NotFound() {
        final UUID id = UUID.randomUUID();
        User updatedUser = new User();
        updatedUser.setEmail("new@example.com");
        updatedUser.setRole(Roles.ADMIN.name());
        updatedUser.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(id, updatedUser))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should deactivate user on delete")
    void testDeleteUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deleteUser(id);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }


    @Test
    @DisplayName("Should return all users")
    void testGetAllUsers() {
        List<User> users = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should get user by email")
    void testGetUserByEmail() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Roles.ADMIN.name());
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUser("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void testGetUserByEmail_NotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("missing@example.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found with email");
    }

    @Test
    @DisplayName("Should add users and return response list")
    void testAddUsers() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setRole(Roles.ADMIN.name());
        user1.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setRole(Roles.USER.name());
        user2.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<UserResponseDto> response = userService.addUsers(List.of(user1, user2));

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getStatus()).isEqualTo("CREATED");
        assertThat(response.get(1).getStatus()).isEqualTo("CREATED");
    }

}
