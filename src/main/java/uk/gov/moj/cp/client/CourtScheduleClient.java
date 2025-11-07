package uk.gov.moj.cp.client;

import com.moj.generated.hmcts.CourtScheduleSchema;
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
public class CourtScheduleClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.amp-url}")
    private String ampUrl;

    @Getter
    @Value("${services.amp-subscription-key}")
    private String ampSubscriptionKey;

    @Getter
    @Value("${services.api-cp-crime-schedulingandlisting-courtschedule.path}")
    private String apiCpCrimeSchedulingandlistingCourtschedulePath;

    protected String buildCourtScheduleUrl(String caseUrn) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpCrimeSchedulingandlistingCourtschedulePath())
            .buildAndExpand(caseUrn)
            .toUriString();
    }

    public ResponseEntity<CourtScheduleSchema> getCourtScheduleByCaseUrn(String token, String caseUrn) {
        try {
            return restTemplate.exchange(
                buildCourtScheduleUrl(caseUrn),
                HttpMethod.GET,
                getRequestEntity(token),
                CourtScheduleSchema.class
            );
        } catch (Exception e) {
            log.error("Error while calling CourtSchedule API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        //headers.setBearerAuth(token);
        headers.setBearerAuth("token");
        headers.set("Ocp-Apim-Subscription-Key", getAmpSubscriptionKey());
        return new HttpEntity<>(headers);
    }
}
