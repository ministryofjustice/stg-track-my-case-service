package uk.gov.moj.cp.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrimeCaseClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.amp-url}")
    private String ampUrl;

    @Getter
    @Value("${services.amp-subscription-key}")
    private String ampSubscriptionKey;

    @Getter
    @Value("${services.crime-cases.url}")
    private String crimeCaseUrl;

    @Getter
    @Value("${services.crime-cases.version}")
    private String crimeCaseVersion;

    @Getter
    @Value("${services.crime-cases.path}")
    private String crimeCasePath;

    protected String buildCrimeCaseUrl(Long caseId) {
        return UriComponentsBuilder
            .fromUriString(getCrimeCaseUrl())
            .pathSegment(getCrimeCaseVersion())
            .path(getCrimeCasePath())
            .buildAndExpand(caseId)
            .toUriString();
    }

    public ResponseEntity<String> getCaseById(Long id) {
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                buildCrimeCaseUrl(id),
                HttpMethod.GET,
                getRequestEntity(),
                String.class
            );
            return res;
        } catch (Exception e) {
            log.error("Error while calling CrimeCase API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
