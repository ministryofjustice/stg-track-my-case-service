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
    @Value("${services.api-cp-refdata-courthearing-courthouses-courtrooms.path}")
    private String apiCpRefdataCourthearingCourthousesCourtroomsPath;

    @Getter
    @Value("${services.api-cp-refdata-courthearing-courthouses.path}")
    private String apiCpRefdataCourthearingCourthousesPath;

    protected String buildCourthearingCourthousesAndCourtRoomsByIdUrl(String courtId, String courtRoomId) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpRefdataCourthearingCourthousesCourtroomsPath())
            .buildAndExpand(courtId, courtRoomId)
            .toUriString();
    }

    protected String buildCourthearingCourthousesByIdUrl(String courtId) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpRefdataCourthearingCourthousesPath())
            .buildAndExpand(courtId)
            .toUriString();
    }


    public ResponseEntity<CourtHouse> getCourtHouseById(String accessToken, String courtId, String courtRoomId) {
        try {
            String courtHouseAmpUrl = (courtRoomId == null || courtRoomId.isEmpty())
                ? buildCourthearingCourthousesByIdUrl(courtId)
                : buildCourthearingCourthousesAndCourtRoomsByIdUrl(courtId, courtRoomId);

            return restTemplate.exchange(
                courtHouseAmpUrl,
                HttpMethod.GET,
                getRequestEntity(accessToken),
                CourtHouse.class
            );
        } catch (Exception e) {
            log.atError().log("Error while calling CourtHouse API", e);
        }
        return null;
    }

    protected HttpEntity<String> getRequestEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);
        headers.set("Ocp-Apim-Subscription-Key", getAmpSubscriptionKey());
        return new HttpEntity<>(headers);
    }

}
