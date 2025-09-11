package uk.gov.moj.cp.client;

import com.moj.generated.hmcts.CourtHouse;
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
public class CourtHouseClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.amp-url}")
    private String ampUrl;

    @Getter
    @Value("${services.amp-subscription-key}")
    private String ampSubscriptionKey;

    @Getter
    @Value("${services.api-cp-refdata-courthearing-courthouses.path}")
    private String apiCpRefdataCourthearingCourthousesPath;

    protected String buildCourthearingCourthousesByIdUrl(String courtId, String courtRoomId) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpRefdataCourthearingCourthousesPath())
            .buildAndExpand(courtId, courtRoomId)
            .toUriString();
    }

    public ResponseEntity<CourtHouse> getCourtHouseById(String courtId, String courtRoomId) {
        try {
            ResponseEntity<CourtHouse> responseEntity = restTemplate.exchange(
                buildCourthearingCourthousesByIdUrl(courtId, courtRoomId),
                HttpMethod.GET,
                getRequestEntity(),
                CourtHouse.class
            );
            return responseEntity;
        } catch (Exception e) {
            log.atError().log("Error while calling CourtHouse API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Ocp-Apim-Subscription-Key", getAmpSubscriptionKey());
        return new HttpEntity<>(headers);
    }

}
