package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.service.UserService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cp.util.ApiUtils.BASIC_TOKEN_PREFIX;
import static uk.gov.moj.cp.util.ApiUtils.BEARER_TOKEN_PREFIX;

@ExtendWith(MockitoExtension.class)
class CaseAuthorizationInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @Mock
    private UserService userService;

    private CaseAuthorizationInterceptor interceptor;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    private static final String VALID_EMAIL = "user@example.com";
    private static final String INVALID_EMAIL = "nonexistent@example.com";
    private static final String NON_CASE_ENDPOINT = "/api/other";
    private static final String message = "{\"message\":\"You are not allowed to access this resource\"}";

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new CaseAuthorizationInterceptor(userService);
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
    }

    @Test
    @DisplayName("Should allow requests to case endpoints with valid Basic auth and active user")
    void testPreHandle_CaseEndpoint_ValidBasicAuth_ActiveUser_ShouldAllow() throws Exception {
        // Given
        User activeUser = createUser(VALID_EMAIL, UserStatus.ACTIVE);
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(VALID_EMAIL)).thenReturn(Optional.of(activeUser));

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with valid Basic auth but deleted user")
    void testPreHandle_CaseEndpoint_ValidBasicAuth_DeletedUser_ShouldReject() throws Exception {
        // Given
        User deletedUser = createUser(VALID_EMAIL, UserStatus.DELETED);
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(VALID_EMAIL)).thenReturn(Optional.of(deletedUser));
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with valid Basic auth but user not found")
    void testPreHandle_CaseEndpoint_ValidBasicAuth_UserNotFound_ShouldReject() throws Exception {
        // Given
        String encodedEmail = Base64.getEncoder().encodeToString(INVALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(INVALID_EMAIL)).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints without authorization header")
    void testPreHandle_CaseEndpoint_NoAuthorizationHeader_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with empty authorization header")
    void testPreHandle_CaseEndpoint_EmptyAuthorizationHeader_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with whitespace-only authorization header")
    void testPreHandle_CaseEndpoint_WhitespaceOnlyAuthorizationHeader_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("   ");
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with authorization header not starting with Basic")
    void testPreHandle_CaseEndpoint_NonBasicAuthorizationHeader_ShouldReject() throws Exception {
        // Given
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + encodedEmail);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with Basic prefix but no token")
    void testPreHandle_CaseEndpoint_BasicOnlyNoToken_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with invalid Base64 encoding")
    void testPreHandle_CaseEndpoint_InvalidBase64Encoding_ShouldReject() throws Exception {
        // Given
        String invalidBase64 = "invalid-base64-string!@#";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + invalidBase64);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should reject requests to case endpoints with empty Base64 token")
    void testPreHandle_CaseEndpoint_EmptyBase64Token_ShouldReject() throws Exception {
        // Given
        String emptyBase64 = Base64.getEncoder().encodeToString("".getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + emptyBase64);
        when(userService.getByEmailIgnoreCase("")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle case sensitivity in Basic token prefix")
    void testPreHandle_CaseEndpoint_LowercaseBasicPrefix_ShouldReject() throws Exception {
        // Given
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("basic " + encodedEmail);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should allow requests to case sub-endpoints with valid auth")
    void testPreHandle_CaseSubEndpoint_ValidAuth_ShouldAllow() throws Exception {
        // Given
        User activeUser = createUser(VALID_EMAIL, UserStatus.ACTIVE);
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(VALID_EMAIL)).thenReturn(Optional.of(activeUser));

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject requests to case sub-endpoints with invalid auth")
    void testPreHandle_CaseSubEndpoint_InvalidAuth_ShouldReject() throws Exception {
        // Given
        String encodedEmail = Base64.getEncoder().encodeToString(INVALID_EMAIL.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(INVALID_EMAIL)).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle email with special characters in Base64 encoding")
    void testPreHandle_CaseEndpoint_EmailWithSpecialCharacters_ShouldAllow() throws Exception {
        // Given
        String specialEmail = "user+test@example-domain.co.uk";
        User activeUser = createUser(specialEmail, UserStatus.ACTIVE);
        String encodedEmail = Base64.getEncoder().encodeToString(specialEmail.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(specialEmail)).thenReturn(Optional.of(activeUser));

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle case insensitive email lookup")
    void testPreHandle_CaseEndpoint_CaseInsensitiveEmail_ShouldAllow() throws Exception {
        // Given
        String upperCaseEmail = "USER@EXAMPLE.COM";
        User activeUser = createUser(VALID_EMAIL, UserStatus.ACTIVE); // User stored with lowercase
        String encodedEmail = Base64.getEncoder().encodeToString(upperCaseEmail.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(upperCaseEmail)).thenReturn(Optional.of(activeUser));

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle very long email address")
    void testPreHandle_CaseEndpoint_VeryLongEmail_ShouldReject() throws Exception {
        // Given
        String veryLongEmail = "a".repeat(200) + "@example.com"; // 200+ character email
        String encodedEmail = Base64.getEncoder().encodeToString(veryLongEmail.getBytes());

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + encodedEmail);
        when(userService.getByEmailIgnoreCase(veryLongEmail)).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle malformed Base64 with invalid characters")
    void testPreHandle_CaseEndpoint_MalformedBase64_ShouldReject() throws Exception {
        // Given
        String malformedBase64 = "YWJjZGVmZ2hpams="; // Valid Base64 but might cause issues

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BASIC_TOKEN_PREFIX + malformedBase64);
        when(userService.getByEmailIgnoreCase("abcdefghijk")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle Basic auth with extra whitespace")
    void testPreHandle_CaseEndpoint_BasicAuthWithWhitespace_ShouldReject() throws Exception {
        // Given
        String encodedEmail = Base64.getEncoder().encodeToString(VALID_EMAIL.getBytes());

        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    private User createUser(String email, UserStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setStatus(status);
        user.setRole(UserRole.USER);
        return user;
    }
}
