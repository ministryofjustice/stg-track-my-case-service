package uk.gov.moj.cp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.moj.cp.client.oauth.OAuthTokenClient;
import uk.gov.moj.cp.model.OAuthTokenResponse;
import uk.gov.moj.cp.model.mock.APIName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(oauthTokenClient.getJwtToken(any(APIName.class))).thenReturn(oauthTokenResponse);

        String accessToken = oauthTokenService.getJwtToken(APIName.SLC);

        assertThat(accessToken).isEqualTo("access-token-value");
        verify(oauthTokenClient, times(1)).getJwtToken(APIName.SLC);
    }

    @Test
    void shouldPropagateExceptionFromTokenClient() {
        when(oauthTokenClient.getJwtToken(any())).thenThrow(new RuntimeException("accessToken retrieval failed"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> oauthTokenService.getJwtToken(APIName.PCD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("accessToken retrieval failed");

        verify(oauthTokenClient, times(1)).getJwtToken(any());
    }
}

