package uk.gov.moj.cp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.moj.cp.Application;
import uk.gov.moj.cp.config.TestCryptoConfig;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;

import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Import(TestCryptoConfig.class)
class UserControllerIntegrationTest {

    private static final String USERS_PATH = "/api/users";
    private static final String AUTH_HEADER_VALUE = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/users - returns all users when email param missing")
    void shouldReturnAllUsersWhenEmailNotProvided() throws Exception {
        String email1 = "allusers1@example.com";
        String email2 = "allusers2@example.com";
        createUserViaEndpoint(email1);
        createUserViaEndpoint(email2);
        updateUserViaEndpoint(email2, UserRole.ADMIN, UserStatus.DELETED);

        mockMvc.perform(get(USERS_PATH).header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].email", hasItem(email1)))
            .andExpect(jsonPath("$[*].email", hasItem(email2)))
            .andExpect(jsonPath("$[?(@.email == '" + email1 + "')].role", hasItem("USER")))
            .andExpect(jsonPath("$[?(@.email == '" + email1 + "')].status", hasItem("ACTIVE")))
            .andExpect(jsonPath("$[?(@.email == '" + email2 + "')].role", hasItem("ADMIN")))
            .andExpect(jsonPath("$[?(@.email == '" + email2 + "')].status", hasItem("DELETED")));
    }

    @Test
    @DisplayName("GET /api/users?email= - returns single user when found")
    void shouldReturnUserWhenEmailProvidedAndFound() throws Exception {
        String email = "getuser@example.com";
        createUserViaEndpoint(email);

        // Test with URL encoded email and different case to verify URL decoding and case insensitivity
        mockMvc.perform(get(USERS_PATH)
                .param("email", "GETUSER%40Example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", equalTo(email)))
            .andExpect(jsonPath("$.role", equalTo("USER")))
            .andExpect(jsonPath("$.status", equalTo("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/users?email= - returns 404 when user not found")
    void shouldReturnNotFoundWhenEmailNotFound() throws Exception {
        mockMvc.perform(get(USERS_PATH)
                .param("email", "missing@example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", equalTo("User not found by email: missing@example.com")));
    }

    @Test
    @DisplayName("POST /api/users/create - creates users and returns response list")
    void shouldCreateUsers() throws Exception {
        createUserViaEndpoint("existing@example.com");
        List<UserDto> request = List.of(
            UserDto.builder().email("new1@example.com").build(),
            UserDto.builder().email("existing@example.com").build()
        );

        mockMvc.perform(post(USERS_PATH + "/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].email", equalTo("new1@example.com")))
            .andExpect(jsonPath("$[0].status", equalTo("CREATED")))
            .andExpect(jsonPath("$[1].email", equalTo("existing@example.com")))
            .andExpect(jsonPath("$[1].status", equalTo("FAILED")))
            .andExpect(jsonPath("$[1].reason", equalTo("Email already exists")));

        // Verify the new user was created by fetching it via endpoint
        mockMvc.perform(get(USERS_PATH)
                .param("email", "new1@example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", equalTo("new1@example.com")));
    }

    @Test
    @DisplayName("PUT /api/users/edit - updates user when service returns response")
    void shouldUpdateUserWhenFound() throws Exception {
        String email = "updateuser@example.com";
        createUserViaEndpoint(email);
        UpdateUserDto updateRequest = UpdateUserDto.builder()
            .email(email)
            .role(UserRole.ADMIN)
            .status(UserStatus.DELETED)
            .build();

        mockMvc.perform(put(USERS_PATH + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", equalTo(email)))
            .andExpect(jsonPath("$.role", equalTo("ADMIN")))
            .andExpect(jsonPath("$.status", equalTo("DELETED")));

        // Verify the update persisted by fetching the user via endpoint
        mockMvc.perform(get(USERS_PATH)
                .param("email", email)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", equalTo(email)))
            .andExpect(jsonPath("$.role", equalTo("ADMIN")))
            .andExpect(jsonPath("$.status", equalTo("DELETED")));
    }

    @Test
    @DisplayName("PUT /api/users/edit - returns 404 when service returns null")
    void shouldReturnNotFoundWhenUpdateFails() throws Exception {
        UpdateUserDto updateRequest = UpdateUserDto.builder()
            .email("missing@example.com")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();

        mockMvc.perform(put(USERS_PATH + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", equalTo("User not found by email: missing@example.com")));
    }

    @Test
    @DisplayName("DELETE /api/users/delete - deletes user when service returns response")
    void shouldDeleteUserWhenFound() throws Exception {
        String email = "deleteuser@example.com";
        createUserViaEndpoint(email);
        UserDto deleteRequest = UserDto.builder()
            .email(email)
            .build();

        mockMvc.perform(delete(USERS_PATH + "/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("DELETED")));

        // Verify the delete persisted by fetching the user via endpoint
        mockMvc.perform(get(USERS_PATH)
                .param("email", email)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", equalTo(email)))
            .andExpect(jsonPath("$.status", equalTo("DELETED")));
    }

    @Test
    @DisplayName("DELETE /api/users/delete - returns 404 when user missing")
    void shouldReturnNotFoundWhenDeleteFails() throws Exception {
        UserDto deleteRequest = UserDto.builder()
            .email("missing@example.com")
            .build();

        mockMvc.perform(delete(USERS_PATH + "/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", equalTo("User not found by email: missing@example.com")));
    }

    private void createUserViaEndpoint(String email) throws Exception {
        List<UserDto> request = List.of(UserDto.builder().email(email).build());
        // Try to create - accept both CREATED (new user) and FAILED (already exists)
        mockMvc.perform(post(USERS_PATH + "/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email", equalTo(email)))
            .andExpect(jsonPath("$[0].status").value(
                anyOf(
                    equalTo("CREATED"),
                    equalTo("FAILED")
                )
            ));
    }

    private void updateUserViaEndpoint(String email, UserRole role, UserStatus status) throws Exception {
        UpdateUserDto updateRequest = UpdateUserDto.builder()
            .email(email)
            .role(role)
            .status(status)
            .build();
        mockMvc.perform(put(USERS_PATH + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk());
    }
}

