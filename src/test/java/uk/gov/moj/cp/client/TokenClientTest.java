package uk.gov.moj.cp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cp.model.TokenResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenClientTest {

    private static final String URL = "https://login.microsoftonline.com";
    private static final String PATH_TEMPLATE = "/{tenant}/oauth2/{version}/token";
    private static final String TENANT_ID = "tenant-id";
    private static final String VERSION = "v2.0";
    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String SCOPE = "api://some-scope/.default";

    private RestTemplate restTemplate;
    private TokenClient tokenClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        tokenClient = new TokenClient(restTemplate) {
            @Override
            public String getUrl() {
                return URL;
            }

            @Override
            public String getPath() {
                return PATH_TEMPLATE;
            }

            @Override
            public String getTenantId() {
                return TENANT_ID;
            }

            @Override
            public String getVersion() {
                return VERSION;
            }

            @Override
            public String getClientId() {
                return CLIENT_ID;
            }

            @Override
            public String getClientSecret() {
                return CLIENT_SECRET;
            }

            @Override
            public String getScope() {
                return SCOPE;
            }
        };
    }

    @Test
    void shouldBuildTokenPathUrl() {
        String expected = "https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token";

        String actual = tokenClient.buildTokenPathUrl(TENANT_ID, VERSION);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldReturnTokenResponse_whenRequestIsSuccessful() {
        TokenResponse tokenResponse = new TokenResponse("Bearer", 3599, 3599, "access-token");
        String expectedUrl = "https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token";

        when(restTemplate.postForEntity(eq(expectedUrl), any(HttpEntity.class), eq(TokenResponse.class)))
            .thenReturn(ResponseEntity.ok(tokenResponse));

        TokenResponse actual = tokenClient.getJwtToken();

        assertThat(actual).isEqualTo(tokenResponse);

        org.mockito.Mockito.verify(restTemplate).postForEntity(
            eq(expectedUrl),
            argThat((HttpEntity<MultiValueMap<String, String>> request) -> {
                assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> body = request.getBody();
                assertThat(body.getFirst("client_id")).isEqualTo(CLIENT_ID);
                assertThat(body.getFirst("client_secret")).isEqualTo(CLIENT_SECRET);
                assertThat(body.getFirst("scope")).isEqualTo(SCOPE);
                assertThat(body.getFirst("grant_type")).isEqualTo("client_credentials");
                return true;
            }),
            eq(TokenResponse.class)
        );
    }

    @Test
    void shouldThrowRuntimeException_whenRequestFails() {
        String expectedUrl = "https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token";

        when(restTemplate.postForEntity(eq(expectedUrl), any(HttpEntity.class), eq(TokenResponse.class)))
            .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());

        assertThatThrownBy(tokenClient::getJwtToken)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to retrieve token");
    }
}

