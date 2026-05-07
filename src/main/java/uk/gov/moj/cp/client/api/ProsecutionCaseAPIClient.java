package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.ProsecutionCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "services.use-mock-data",
    havingValue = "false",
    matchIfMissing = true
)
public class ProsecutionCaseAPIClient implements ProsecutionCaseClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.amp-url}")
    private String ampUrl;

    @Getter
    @Value("${services.pcd-amp-subscription-key}")
    private String pcdAmpSubscriptionKey;

    @Getter
    @Value("${services.api-cp-pcd-prosecution-case-details.path}")
    private String apiCpApiCpPcdCourtStatusPath;

    protected String buildCourtScheduleUrl(final String caseUrn) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpApiCpPcdCourtStatusPath())
            .buildAndExpand(caseUrn)
            .toUriString();
    }

    public ResponseEntity<ProsecutionCase> getCaseDetails(final String accessToken, final String caseUrn) {
        try {
            return restTemplate.exchange(
                buildCourtScheduleUrl(caseUrn),
                HttpMethod.GET,
                getRequestEntity(accessToken),
                ProsecutionCase.class
            );
        } catch (HttpStatusCodeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error while calling ProsecutionCase API: caseUrn: {}, exception: {}",
                      caseUrn, e.getMessage());
            throw e;
        }
    }

    protected HttpEntity<String> getRequestEntity(final String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);
        headers.set("Ocp-Apim-Subscription-Key", getPcdAmpSubscriptionKey());
        return new HttpEntity<>(headers);
    }
}
