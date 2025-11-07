package uk.gov.moj.cp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.client.TokenClient;
import uk.gov.moj.cp.model.TokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    @Mock
    private TokenClient tokenClient;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnAccessTokenFromClientResponse() {
        TokenResponse tokenResponse = new TokenResponse("Bearer", 3600, 3600, "access-token-value");
        when(tokenClient.getJwtToken()).thenReturn(tokenResponse);

        String accessToken = tokenService.getJwtToken();

        assertThat(accessToken).isEqualTo("access-token-value");
        verify(tokenClient, times(1)).getJwtToken();
    }

    @Test
    void shouldPropagateExceptionFromTokenClient() {
        when(tokenClient.getJwtToken()).thenThrow(new RuntimeException("Token retrieval failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(tokenService::getJwtToken)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Token retrieval failed");

        verify(tokenClient, times(1)).getJwtToken();
    }
}

