package uk.gov.moj.cp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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


class UserControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userController = new UserController(userService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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

        mockMvc.perform(delete("/api/user")
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

        mockMvc.perform(get("/api/user")
                            .queryParam("email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
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

        mockMvc.perform(post("/api/users")
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
    @DisplayName("POST /api/user - create user")
    void testCreateUser() throws Exception {
        final UserDto userDto = UserDto.builder()
            .email("test@example.com")
            .build();

        final UserCreationResponseDto responseDto = UserCreationResponseDto.builder()
            .email("test@example.com")
            .status(UserCreationStatus.CREATED)
            .reason("User created successfully")
            .build();

        when(userService.createUser(any(UserDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.reason").value("User created successfully"));
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

        mockMvc.perform(put("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateUserDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

}
