package uk.gov.moj.cp.client;

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

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrimeCaseClient {

    private final RestTemplate restTemplate;

    @Value("${services.crime-cases.url}")
    private String crimeCaseUrl;

    @Value("${services.crime-cases.version}")
    private String crimeCaseVersion;

    private static final String CASE_RESULTS_PATH = "cases/{id}/results";

    protected String buildCrimeCaseUrl(Long id) {
        return UriComponentsBuilder
            .fromUri(URI.create(crimeCaseUrl))
            .pathSegment(crimeCaseVersion)
            .pathSegment(CASE_RESULTS_PATH)
            .buildAndExpand(id)
            .toUriString();
    }

    public ResponseEntity<String> getCaseById(Long id) {
        try {
            return restTemplate.exchange(
                buildCrimeCaseUrl(id),
                HttpMethod.GET,
                getRequestEntity(),
                String.class
            );
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
