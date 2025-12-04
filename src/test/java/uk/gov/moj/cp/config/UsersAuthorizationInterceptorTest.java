package uk.gov.moj.cp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ResponseFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cp.util.ApiUtils.BEARER_TOKEN_PREFIX;

@ExtendWith(MockitoExtension.class)
class UsersAuthorizationInterceptorTest {

    @Mock
    private HttpServletRequest request;

    private HttpServletResponse response;

    @Mock
    private Object handler;

    private UsersAuthorizationInterceptor interceptor;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    private static final String VALID_TOKEN = "valid-accessToken-123";
    private static final String INVALID_TOKEN = "invalid-accessToken-456";
    private static final String NON_USERS_ENDPOINT = "/api/other";
    private static final String message = "{\"message\":\"You are not allowed to access this resource\"}";

    @BeforeEach
    void setUp() throws IOException {
        interceptor = new UsersAuthorizationInterceptor(VALID_TOKEN);
        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
        response = mock(ResponseFacade.class);
    }

    @Test
    @DisplayName("Should allow requests to users endpoints with valid Bearer accessToken")
    void testPreHandle_UsersEndpoint_ValidBearerToken_ShouldAllow() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject requests to users endpoints with invalid Bearer accessToken")
    void testPreHandle_UsersEndpoint_InvalidBearerToken_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + INVALID_TOKEN);
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
    @DisplayName("Should reject requests to users endpoints without authorization header")
    void testPreHandle_UsersEndpoint_NoAuthorizationHeader_ShouldReject() throws Exception {
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
    @DisplayName("Should reject requests to users endpoints with empty authorization header")
    void testPreHandle_UsersEndpoint_EmptyAuthorizationHeader_ShouldReject() throws Exception {
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
    @DisplayName("Should reject requests to users endpoints with whitespace-only authorization header")
    void testPreHandle_UsersEndpoint_WhitespaceOnlyAuthorizationHeader_ShouldReject() throws Exception {
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
    @DisplayName("Should reject requests to users endpoints with authorization header not starting with Bearer")
    void testPreHandle_UsersEndpoint_NonBearerAuthorizationHeader_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + VALID_TOKEN);
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
    @DisplayName("Should reject requests to users endpoints with Bearer accessToken but no actual accessToken")
    void testPreHandle_UsersEndpoint_BearerOnlyNoToken_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX);
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
    @DisplayName("Should reject requests to users endpoints with Bearer accessToken and extra whitespace")
    void testPreHandle_UsersEndpoint_BearerTokenWithWhitespace_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + " " + VALID_TOKEN);
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
    @DisplayName("Should reject requests to users endpoints with Bearer accessToken and leading/trailing whitespace")
    void testPreHandle_UsersEndpoint_BearerTokenWithLeadingTrailingWhitespace_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(" " + BEARER_TOKEN_PREFIX + VALID_TOKEN + " ");
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
    @DisplayName("Should handle case sensitivity in Bearer accessToken prefix")
    void testPreHandle_UsersEndpoint_LowercaseBearerPrefix_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("bearer " + VALID_TOKEN);
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
    @DisplayName("Should allow requests to users sub-endpoints with valid accessToken")
    void testPreHandle_UsersSubEndpoint_ValidToken_ShouldAllow() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN);

        // When
        boolean result = interceptor.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject requests to users sub-endpoints with invalid accessToken")
    void testPreHandle_UsersSubEndpoint_InvalidToken_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + INVALID_TOKEN);
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
    @DisplayName("Should handle interceptor with null required header value")
    void testPreHandle_UsersEndpoint_NullRequiredHeaderValue_ShouldReject() throws Exception {
        // Given
        UsersAuthorizationInterceptor interceptorWithNullToken = new UsersAuthorizationInterceptor(null);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptorWithNullToken.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle interceptor with empty required header value")
    void testPreHandle_UsersEndpoint_EmptyRequiredHeaderValue_ShouldReject() throws Exception {
        // Given
        UsersAuthorizationInterceptor interceptorWithEmptyToken = new UsersAuthorizationInterceptor("");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptorWithEmptyToken.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle interceptor with whitespace-only required header value")
    void testPreHandle_UsersEndpoint_WhitespaceOnlyRequiredHeaderValue_ShouldReject() throws Exception {
        // Given
        UsersAuthorizationInterceptor interceptorWithWhitespaceToken = new UsersAuthorizationInterceptor("   ");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = interceptorWithWhitespaceToken.preHandle(request, response, handler);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        printWriter.flush();
        assertThat(responseWriter.toString()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle exact accessToken match - case sensitive")
    void testPreHandle_UsersEndpoint_CaseSensitiveTokenMatch_ShouldReject() throws Exception {
        // Given
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + VALID_TOKEN.toUpperCase());
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
    @DisplayName("Should handle accessToken with special characters")
    void testPreHandle_UsersEndpoint_TokenWithSpecialCharacters_ShouldAllow() throws Exception {
        // Given
        String specialToken = "accessToken-with-special.chars_123!@#";
        UsersAuthorizationInterceptor interceptorWithSpecialToken = new UsersAuthorizationInterceptor(specialToken);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + specialToken);

        // When
        boolean result = interceptorWithSpecialToken.preHandle(request, response, handler);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle very long accessToken")
    void testPreHandle_UsersEndpoint_VeryLongToken_ShouldReject() throws Exception {
        // Given
        String veryLongToken = "a".repeat(1000);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_TOKEN_PREFIX + veryLongToken);
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
    @DisplayName("Should handle empty accessToken")
    void testPreHandle_UsersEndpoint_EmptyToken_ShouldReject() throws Exception {
        // Given
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
}
