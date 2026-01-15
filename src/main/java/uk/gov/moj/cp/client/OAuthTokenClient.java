package uk.gov.moj.cp.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.moj.cp.model.OAuthTokenResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.oauth-token.url}")
    private String url;

    @Getter
    @Value("${services.oauth-token.path}")
    private String path;

    @Getter
    @Value("${services.oauth-token.tenant-id}")
    private String tenantId;

    @Getter
    @Value("${services.oauth-token.version}")
    private String version;

    @Getter
    @Value("${services.oauth-token.client-id}")
    private String clientId;

    @Getter
    @Value("${services.oauth-token.client-secret}")
    private String clientSecret;

    @Getter
    @Value("${services.oauth-token.scope}")
    private String scope;


    protected String buildTokenPathUrl(String tenantId, String version) {
        return UriComponentsBuilder
            .fromUriString(getUrl())
            .path(getPath())
            .buildAndExpand(tenantId, version)
            .toUriString();
    }

    public OAuthTokenResponse getJwtToken() {
        HttpEntity<MultiValueMap<String, String>> request = getHttpEntity();
        ResponseEntity<OAuthTokenResponse> response = restTemplate.postForEntity(
            buildTokenPathUrl(getTenantId(), getVersion()),
            request, OAuthTokenResponse.class
        );
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            log.error("Failed to retrieve token. HTTP Status: {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve accessToken");
        }
    }

    private HttpEntity<MultiValueMap<String, String>> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", getClientId());
        body.add("client_secret", getClientSecret());
        body.add("scope", getScope());
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return request;
    }
}
