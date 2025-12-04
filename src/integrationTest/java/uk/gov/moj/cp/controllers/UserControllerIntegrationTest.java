package uk.gov.moj.cp.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
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
import uk.gov.moj.cp.dto.ErrorResponseDto;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.dto.UserCreationResponseDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.UserCreationStatus;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.moj.cp.config.ApiPaths.PATH_API_USERS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Import(TestCryptoConfig.class)
class UserControllerIntegrationTest {

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

        MvcResult result = mockMvc.perform(get(PATH_API_USERS).header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        List<UserResponseDto> users = readResponseList(
            result,
            new TypeReference<List<UserResponseDto>>() {}
        );

        assertThat(users).extracting(UserResponseDto::getEmail)
            .contains(email1, email2);

        UserResponseDto user1 = users.stream()
            .filter(u -> email1.equals(u.getEmail()))
            .findFirst()
            .orElseThrow();
        assertThat(user1.getRole()).isEqualTo(UserRole.USER);
        assertThat(user1.getStatus()).isEqualTo(UserStatus.ACTIVE);

        UserResponseDto user2 = users.stream()
            .filter(u -> email2.equals(u.getEmail()))
            .findFirst()
            .orElseThrow();
        assertThat(user2.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user2.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("GET /api/users?email= - returns single user when found")
    void shouldReturnUserWhenEmailProvidedAndFound() throws Exception {
        String email = "getuser@example.com";
        createUserViaEndpoint(email);

        // Test with URL encoded email and different case to verify URL decoding and case insensitivity
        MvcResult result = mockMvc.perform(get(PATH_API_USERS)
                .param("email", "GETUSER%40Example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto response = readResponse(result, UserResponseDto.class);
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /api/users?email= - returns 404 when user not found")
    void shouldReturnNotFoundWhenEmailNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get(PATH_API_USERS)
                .param("email", "missing@example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andReturn();

        ErrorResponseDto response = readResponse(result, ErrorResponseDto.class);
        assertThat(response.message()).isEqualTo("User not found by email: missing@example.com");
    }

    @Test
    @DisplayName("POST /api/users/create - creates users and returns response list")
    void shouldCreateUsers() throws Exception {
        createUserViaEndpoint("existing@example.com");
        List<UserDto> request = List.of(
            UserDto.builder().email("new1@example.com").build(),
            UserDto.builder().email("existing@example.com").build()
        );

        MvcResult result = mockMvc.perform(post(PATH_API_USERS + "/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        List<UserCreationResponseDto> responses = readResponseList(
            result,
            new TypeReference<List<UserCreationResponseDto>>() {}
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmail()).isEqualTo("new1@example.com");
        assertThat(responses.get(0).getStatus()).isEqualTo(UserCreationStatus.CREATED);
        assertThat(responses.get(1).getEmail()).isEqualTo("existing@example.com");
        assertThat(responses.get(1).getStatus()).isEqualTo(UserCreationStatus.FAILED);
        assertThat(responses.get(1).getReason()).isEqualTo("Email already exists");

        // Verify the new user was created by fetching it via endpoint
        MvcResult getUserResult = mockMvc.perform(get(PATH_API_USERS)
                .param("email", "new1@example.com")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto userResponse = readResponse(getUserResult, UserResponseDto.class);
        assertThat(userResponse.getEmail()).isEqualTo("new1@example.com");
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

        MvcResult result = mockMvc.perform(put(PATH_API_USERS + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto updateResponse = readResponse(result, UserResponseDto.class);
        assertThat(updateResponse.getEmail()).isEqualTo(email);
        assertThat(updateResponse.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(updateResponse.getStatus()).isEqualTo(UserStatus.DELETED);

        // Verify the update persisted by fetching the user via endpoint
        MvcResult getUserResult = mockMvc.perform(get(PATH_API_USERS)
                .param("email", email)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto userResponse = readResponse(getUserResult, UserResponseDto.class);
        assertThat(userResponse.getEmail()).isEqualTo(email);
        assertThat(userResponse.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userResponse.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("PUT /api/users/edit - returns 404 when service returns null")
    void shouldReturnNotFoundWhenUpdateFails() throws Exception {
        UpdateUserDto updateRequest = UpdateUserDto.builder()
            .email("missing@example.com")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();

        MvcResult result = mockMvc.perform(put(PATH_API_USERS + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andReturn();

        ErrorResponseDto response = readResponse(result, ErrorResponseDto.class);
        assertThat(response.message()).isEqualTo("User not found by email: missing@example.com");
    }

    @Test
    @DisplayName("DELETE /api/users/delete - deletes user when service returns response")
    void shouldDeleteUserWhenFound() throws Exception {
        String email = "deleteuser@example.com";
        createUserViaEndpoint(email);
        UserDto deleteRequest = UserDto.builder()
            .email(email)
            .build();

        MvcResult result = mockMvc.perform(delete(PATH_API_USERS + "/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto response = readResponse(result, UserResponseDto.class);
        assertThat(response.getStatus()).isEqualTo(UserStatus.DELETED);

        // Verify the delete persisted by fetching the user via endpoint
        MvcResult getUserResult = mockMvc.perform(get(PATH_API_USERS)
                .param("email", email)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        UserResponseDto userResponse = readResponse(getUserResult, UserResponseDto.class);
        assertThat(userResponse.getEmail()).isEqualTo(email);
        assertThat(userResponse.getStatus()).isEqualTo(UserStatus.DELETED);
    }

    @Test
    @DisplayName("DELETE /api/users/delete - returns 404 when user missing")
    void shouldReturnNotFoundWhenDeleteFails() throws Exception {
        UserDto deleteRequest = UserDto.builder()
            .email("missing@example.com")
            .build();

        MvcResult result = mockMvc.perform(delete(PATH_API_USERS + "/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deleteRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isNotFound())
            .andReturn();

        ErrorResponseDto response = readResponse(result, ErrorResponseDto.class);
        assertThat(response.message()).isEqualTo("User not found by email: missing@example.com");
    }

    private void createUserViaEndpoint(String email) throws Exception {
        List<UserDto> request = List.of(UserDto.builder().email(email).build());
        MvcResult result = mockMvc.perform(post(PATH_API_USERS + "/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk())
            .andReturn();

        List<UserCreationResponseDto> responses = readResponseList(
            result,
            new TypeReference<List<UserCreationResponseDto>>() {}
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEmail()).isEqualTo(email);
        assertThat(responses.get(0).getStatus()).isIn(UserCreationStatus.CREATED, UserCreationStatus.FAILED);
    }

    private void updateUserViaEndpoint(String email, UserRole role, UserStatus status) throws Exception {
        UpdateUserDto updateRequest = UpdateUserDto.builder()
            .email(email)
            .role(role)
            .status(status)
            .build();
        mockMvc.perform(put(PATH_API_USERS + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE))
            .andExpect(status().isOk());
    }

    private <T> T readResponse(MvcResult result, Class<T> clazz) throws Exception {
        return objectMapper.readValue(
            result.getResponse().getContentAsString(),
            clazz
        );
    }

    private <T> List<T> readResponseList(MvcResult result, TypeReference<List<T>> typeReference) throws Exception {
        return objectMapper.readValue(
            result.getResponse().getContentAsString(),
            typeReference
        );
    }
}

