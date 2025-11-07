package uk.gov.moj.cp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.client.OAuthTokenClient;
import uk.gov.moj.cp.model.OAuthTokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthTokenServiceTest {

    @Mock
    private OAuthTokenClient oauthTokenClient;

    @InjectMocks
    private OAuthTokenService oauthTokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnAccessTokenFromClientResponse() {
        OAuthTokenResponse oauthTokenResponse = new OAuthTokenResponse("Bearer", 3600, 3600, "access-token-value");
        when(oauthTokenClient.getJwtToken()).thenReturn(oauthTokenResponse);

        String accessToken = oauthTokenService.getJwtToken();

        assertThat(accessToken).isEqualTo("access-token-value");
        verify(oauthTokenClient, times(1)).getJwtToken();
    }

    @Test
    void shouldPropagateExceptionFromTokenClient() {
        when(oauthTokenClient.getJwtToken()).thenThrow(new RuntimeException("accessToken retrieval failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(oauthTokenService::getJwtToken)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("accessToken retrieval failed");

        verify(oauthTokenClient, times(1)).getJwtToken();
    }
}

