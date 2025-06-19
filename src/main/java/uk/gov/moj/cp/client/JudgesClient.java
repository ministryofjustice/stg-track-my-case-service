package uk.gov.moj.cp.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgesClient {

    private final RestTemplate restTemplate;

    @Value("${services.refdata-courthearing-judges.url}")
    private String judgesUrl;

    @Value("${services.refdata-courthearing-judges.version}")
    private String judgesVersion;

    private static final String JUDGES_BY_ID_PATH = "judges/{id}";

    protected String buildJudgesUrl(Long id) {
        return UriComponentsBuilder
            .fromUri(URI.create(judgesUrl))
            .pathSegment(judgesVersion)
            .pathSegment(JUDGES_BY_ID_PATH)
            .buildAndExpand(id)
            .toUriString();
    }

    public ResponseEntity<String> getJudgesById(Long id) {
        try {
            return restTemplate.exchange(
                buildJudgesUrl(id),
                HttpMethod.GET,
                getRequestEntity(),
                String.class
            );
        } catch (Exception e) {
            log.error("Error while calling Judges API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
