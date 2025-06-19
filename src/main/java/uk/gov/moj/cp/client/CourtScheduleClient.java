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
public class CourtScheduleClient {

    private final RestTemplate restTemplate;

    @Value("${services.crime-schedulingandlisting-courtschedule.url}")
    private String courtScheduleUrl;

    @Value("${services.crime-schedulingandlisting-courtschedule.version}")
    private String courtScheduleVersion;

    private static final String COURT_SCHEDULE_PATH = "case/{caseUrn}/courtschedule";

    protected String buildCourtScheduleUrl(String caseUrn) {
        return UriComponentsBuilder
            .fromUri(URI.create(courtScheduleUrl))
            .pathSegment(courtScheduleVersion)
            .pathSegment(COURT_SCHEDULE_PATH)
            .buildAndExpand(caseUrn)
            .toString();
    }

    public ResponseEntity<String> getCourtScheduleByCaseUrn(String caseUrn) {
        try {
            return restTemplate.exchange(
                buildCourtScheduleUrl(caseUrn),
                HttpMethod.GET,
                getRequestEntity(),
                String.class
            );
        } catch (Exception e) {
            log.error("Error while calling CourtSchedule API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
