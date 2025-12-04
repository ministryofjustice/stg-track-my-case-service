package uk.gov.moj.cp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.moj.cp.dto.UserCreationResponseDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.model.UserCreationStatus;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.moj.cp.util.Utils.objectMapper;

class UserControllerTest {
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("GET /api/users - get all users")
    void testGetAllUsers() throws Exception {
        final UserResponseDto u1 = UserResponseDto.builder()
            .email("user1@example.com")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .updated(java.time.LocalDateTime.now())
            .build();

        final UserResponseDto u2 = UserResponseDto.builder()
            .email("user2@example.com")
            .role(UserRole.ADMIN)
            .status(UserStatus.DELETED)
            .updated(java.time.LocalDateTime.now())
            .build();

        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value("user1@example.com"))
            .andExpect(jsonPath("$[0].role").value("USER"))
            .andExpect(jsonPath("$[1].email").value("user2@example.com"))
            .andExpect(jsonPath("$[1].role").value("ADMIN"));
    }

    @Test
    @DisplayName("DELETE /api/user - delete user")
    void testDeleteUser() throws Exception {
        final UserDto userDto = UserDto.builder()
            .email("test@example.com")
            .build();

        final UserResponseDto deletedUser = UserResponseDto.builder()
            .email("test@example.com")
            .role(UserRole.USER)
            .status(UserStatus.DELETED)
            .updated(java.time.LocalDateTime.now())
            .build();

        when(userService.deleteUser(userDto)).thenReturn(deletedUser);

        mockMvc.perform(delete("/api/users/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    @DisplayName("GET /api/user - get user by email")
    void testGetUserByEmail() throws Exception {
        final UserResponseDto user = UserResponseDto.builder()
            .email("test@example.com")
            .role(UserRole.ADMIN)
            .status(UserStatus.ACTIVE)
            .updated(java.time.LocalDateTime.now())
            .build();

        when(userService.getUser("test@example.com")).thenReturn(user);

        mockMvc.perform(get("/api/users")
                            .param("email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/user - should return error when user is not found by email")
    void testGetUserByEmail_UserNotFound() throws Exception {
        when(userService.getUser("missing@example.com")).thenReturn(null);

        mockMvc.perform(get("/api/users")
                            .param("email", "missing@example.com"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found by email: missing@example.com"));
    }

    @Test
    @DisplayName("GET /api/user - should handle URL encoded email parameter")
    void testGetUserByEmail_UrlEncodedEmail() throws Exception {
        final UserResponseDto user = UserResponseDto.builder()
            .email("test+user@example.com")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .updated(java.time.LocalDateTime.now())
            .build();

        when(userService.getUser("test+user@example.com")).thenReturn(user);

        // URL encode the email parameter
        mockMvc.perform(get("/api/users")
                            .param("email", "test%2Buser%40example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test+user@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/users - add multiple users")
    void testCreateUsers() throws Exception {
        final UserDto user1 = UserDto.builder()
            .email("u1@example.com")
            .build();

        final UserDto user2 = UserDto.builder()
            .email("u2@example.com")
            .build();
        List<UserDto> users = List.of(user1, user2);

        List<UserCreationResponseDto> responseDtos = List.of(
            UserCreationResponseDto.builder()
                .email("u1@example.com")
                .status(UserCreationStatus.CREATED)
                .reason("Success")
                .build(),
            UserCreationResponseDto.builder()
                .email("u2@example.com")
                .status(UserCreationStatus.CREATED)
                .reason("User created successfully")
                .build()
        );

        when(userService.addUsers(eq(users))).thenReturn(responseDtos);

        mockMvc.perform(post("/api/users/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(users)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("CREATED"))
            .andExpect(jsonPath("$[0].email").value("u1@example.com"))
            .andExpect(jsonPath("$[1].status").value("CREATED"))
            .andExpect(jsonPath("$[1].email").value("u2@example.com"));
    }

    @Test
    @DisplayName("PUT /api/user - update user")
    void testUpdateUser() throws Exception {
        final UpdateUserDto updateUserDto = UpdateUserDto.builder()
            .email("test@example.com")
            .status(UserStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build();

        final UserResponseDto updatedUser = UserResponseDto.builder()
            .email("test@example.com")
            .role(UserRole.ADMIN)
            .status(UserStatus.ACTIVE)
            .updated(java.time.LocalDateTime.now())
            .build();

        when(userService.updateUser(any(UpdateUserDto.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/edit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/user - should return error when user is not found")
    void testUpdateUser_UserNotFound() throws Exception {
        final UpdateUserDto updateUserDto = UpdateUserDto.builder()
            .email("missing@example.com")
            .status(UserStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build();

        when(userService.updateUser(any(UpdateUserDto.class))).thenReturn(null);

        mockMvc.perform(put("/api/users/edit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserDto)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found by email: missing@example.com"));
    }

    @Test
    @DisplayName("DELETE /api/user - should return error when user is not found")
    void testDeleteUser_UserNotFound() throws Exception {
        final UserDto userDto = UserDto.builder()
            .email("missing@example.com")
            .build();

        when(userService.deleteUser(userDto)).thenReturn(null);

        mockMvc.perform(delete("/api/users/delete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found by email: missing@example.com"));
    }

    @Test
    @DisplayName("POST /api/users - should handle mixed success and failure results")
    void testCreateUsers_MixedResults() throws Exception {
        final UserDto user1 = UserDto.builder()
            .email("valid@example.com")
            .build();

        final UserDto user2 = UserDto.builder()
            .email("invalid-email")
            .build();

        List<UserDto> users = List.of(user1, user2);

        List<UserCreationResponseDto> responseDtos = List.of(
            UserCreationResponseDto.builder()
                .email("valid@example.com")
                .status(UserCreationStatus.CREATED)
                .reason("User created successfully")
                .build(),
            UserCreationResponseDto.builder()
                .email("invalid-email")
                .status(UserCreationStatus.FAILED)
                .reason("User email validation failed")
                .build()
        );

        when(userService.addUsers(eq(users))).thenReturn(responseDtos);

        mockMvc.perform(post("/api/users/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(users)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("CREATED"))
            .andExpect(jsonPath("$[0].email").value("valid@example.com"))
            .andExpect(jsonPath("$[1].status").value("FAILED"))
            .andExpect(jsonPath("$[1].email").value("invalid-email"));
    }

    @Test
    @DisplayName("GET /api/user - should handle null email parameter")
    void testGetUserByEmail_WrongEmailParam() throws Exception {
        mockMvc.perform(get("/api/users?email=q"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found by email: q"));
    }
}
