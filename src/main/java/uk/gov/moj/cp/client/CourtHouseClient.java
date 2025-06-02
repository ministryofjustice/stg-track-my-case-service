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
public class CourtHouseClient {

    private final RestTemplate restTemplate;

    @Value("${services.courthearing-courthouses.url}")
    private String courthearingCourthousesUrl;

    @Value("${services.courthearing-courthouses.version}")
    private String getCourthearingCourthousesVersion;

    private static final String COURTHOUSES_BY_ID = "courthouses/{id}";

    protected String getCourthearingCourthousesById(String id) {
        return UriComponentsBuilder
            .fromUri(URI.create(courthearingCourthousesUrl))
            .pathSegment(getCourthearingCourthousesVersion)
            .pathSegment(COURTHOUSES_BY_ID)
            .buildAndExpand(id)
            .toUriString();
    }

    public ResponseEntity<String> getCourtHouseById(String id) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                getCourthearingCourthousesById(id),
                HttpMethod.GET,
                getRequestEntity(),
                String.class
            );
            return responseEntity;
        } catch (Exception e) {
            log.error("Error while calling CourtHouse API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

}
