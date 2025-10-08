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
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.ActiveStatus;
import uk.gov.moj.cp.model.Roles;
import uk.gov.moj.cp.model.User;
import uk.gov.moj.cp.service.UserService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("GET /api/user - get all users")
    void testGetAllUsers() throws Exception {
        final User u1 = new User();
        u1.setEmail("user1@example.com");
        u1.setRole(Roles.USER.name());
        u1.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        final User u2 = new User();
        u2.setId(UUID.randomUUID());
        u2.setEmail("user2@example.com");
        u2.setRole(Roles.ADMIN.name());
        u2.setActive(Boolean.valueOf(ActiveStatus.FALSE.name()));

        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].email").value("user1@example.com"));
    }

    @Test
    @DisplayName("DELETE /api/user/{id} - delete user")
    void testDeleteUser() throws Exception {
        final UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/user/{id}", id))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/user/{email} - get user by email")
    void testGetUserByEmail() throws Exception {
        final User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Roles.ADMIN.name());
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userService.getUser("test@example.com")).thenReturn(user);

        mockMvc.perform(get("/api/user/{email}", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/user/bulk - add multiple users")
    void testCreateUsers() throws Exception {
        final User user1 = new User();
        user1.setEmail("u1@example.com");
        user1.setRole(Roles.ADMIN.name());
        user1.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        final User user2 = new User();
        user2.setEmail("u2@example.com");
        user2.setRole(Roles.USER.name());
        user2.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        List<UserResponseDto> responseDtos = List.of(
            new UserResponseDto("u1@example.com", "CREATED", "Success"),
            new UserResponseDto("u2@example.com", "CREATED", "reason")
        );

        when(userService.addUsers(any())).thenReturn(responseDtos);

        mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(user1, user2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("CREATED"));
    }

    @Test
    @DisplayName("POST /api/user - create user")
    void testCreateUser() throws Exception {
        final User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Roles.ADMIN.name());
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));

        when(userService.createUser(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

}
