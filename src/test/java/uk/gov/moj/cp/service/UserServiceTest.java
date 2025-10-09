package uk.gov.moj.cp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.dto.UserCreationResponseDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.UserCreationStatus;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.repository.UserRepository;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        UserDto userDto = UserDto.builder()
            .email("test@example.com")
            .build();

        User savedUser = new User("test@example.com");
        savedUser.setRole(UserRole.USER);
        savedUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserCreationResponseDto result = userService.createUser(userDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo(UserCreationStatus.CREATED);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should return failed response for invalid email")
    void testCreateUser_InvalidEmail() {
        UserDto userDto = UserDto.builder()
            .email("invalid-email") // invalid email format
            .build();

        UserCreationResponseDto result = userService.createUser(userDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("invalid-email");
        assertThat(result.getStatus()).isEqualTo(UserCreationStatus.FAILED);
        assertThat(result.getReason()).isEqualTo("User email validation failed");
    }

    @Test
    @DisplayName("Should return failed response for duplicate email")
    void testCreateUser_DuplicateEmail() {
        UserDto userDto = UserDto.builder()
            .email("existing@example.com")
            .build();

        User existingUser = new User("existing@example.com");
        when(userRepository.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(existingUser));

        UserCreationResponseDto result = userService.createUser(userDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("existing@example.com");
        assertThat(result.getStatus()).isEqualTo(UserCreationStatus.FAILED);
        assertThat(result.getReason()).isEqualTo("Email already exists");
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("Should update existing user")
    void testUpdateUser_Success() {
        UpdateUserDto updateUserDto = UpdateUserDto.builder()
            .email("test@example.com")
            .role(UserRole.ADMIN)
            .status(UserStatus.DELETED)
            .build();

        User existingUser = new User("test@example.com");
        existingUser.setRole(UserRole.USER);
        existingUser.setStatus(UserStatus.ACTIVE);

        User updatedUser = new User("test@example.com");
        updatedUser.setRole(UserRole.ADMIN);
        updatedUser.setStatus(UserStatus.DELETED);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserResponseDto result = userService.updateUser(updateUserDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should return null when updating non-existent user")
    void testUpdateUser_NotFound() {
        UpdateUserDto updateUserDto = UpdateUserDto.builder()
            .email("missing@example.com")
            .role(UserRole.ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        UserResponseDto result = userService.updateUser(updateUserDto);

        assertThat(result).isNull();
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("Should mark user as deleted on delete")
    void testDeleteUser() {
        UserDto userDto = UserDto.builder()
            .email("test@example.com")
            .build();

        User existingUser = new User("test@example.com");
        existingUser.setStatus(UserStatus.ACTIVE);

        User deletedUser = new User("test@example.com");
        deletedUser.setStatus(UserStatus.DELETED);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(deletedUser);

        UserResponseDto result = userService.deleteUser(userDto);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should return null when deleting non-existent user")
    void testDeleteUser_NotFound() {
        UserDto userDto = UserDto.builder()
            .email("missing@example.com")
            .build();

        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        UserResponseDto result = userService.deleteUser(userDto);

        assertThat(result).isNull();
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("Should return all users")
    void testGetAllUsers() {
        User user1 = new User("user1@example.com");
        user1.setRole(UserRole.USER);
        user1.setStatus(UserStatus.ACTIVE);

        User user2 = new User("user2@example.com");
        user2.setRole(UserRole.ADMIN);
        user2.setStatus(UserStatus.ACTIVE);

        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        List<UserResponseDto> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.USER);
        assertThat(result.get(1).getEmail()).isEqualTo("user2@example.com");
        assertThat(result.get(1).getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should get user by email")
    void testGetUserByEmail() {
        User user = new User("test@example.com");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        UserResponseDto result = userService.getUser("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return null when user not found by email")
    void testGetUserByEmail_NotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        UserResponseDto result = userService.getUser("missing@example.com");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null for invalid email in getUser")
    void testGetUserByEmail_InvalidEmail() {
        UserResponseDto result = userService.getUser("invalid-email");

        assertThat(result).isNull();
        verify(userRepository, times(0)).findByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Should add users and return response list")
    void testAddUsers() {
        UserDto userDto1 = UserDto.builder()
            .email("user1@example.com")
            .build();

        UserDto userDto2 = UserDto.builder()
            .email("user2@example.com")
            .build();

        User savedUser1 = new User("user1@example.com");
        savedUser1.setRole(UserRole.USER);
        savedUser1.setStatus(UserStatus.ACTIVE);

        User savedUser2 = new User("user2@example.com");
        savedUser2.setRole(UserRole.USER);
        savedUser2.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("user1@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<UserCreationResponseDto> response = userService.addUsers(List.of(userDto1, userDto2));

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getStatus()).isEqualTo(UserCreationStatus.CREATED);
        assertThat(response.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(response.get(1).getStatus()).isEqualTo(UserCreationStatus.CREATED);
        assertThat(response.get(1).getEmail()).isEqualTo("user2@example.com");
    }

}
