package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.CourtHouse;
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
public class CourtHouseAPIClient implements CourtHouseClient {

    private final RestTemplate restTemplate;

    @Getter
    @Value("${services.amp-url}")
    private String ampUrl;

    @Getter
    @Value("${services.amp-subscription-key}")
    private String ampSubscriptionKey;

    @Getter
    @Value("${services.api-cp-refdata-courthearing-courthouses-courtrooms.path}")
    private String apiCpRefDataCourtHearingCourtHousesCourtroomsPath;

    @Getter
    @Value("${services.api-cp-refdata-courthearing-courthouses.path}")
    private String apiCpRefDataCourtHearingCourtHousesPath;


    protected String buildCourtHearingCourtHousesAndCourtRoomsByIdUrl(String courtId, String courtRoomId) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpRefDataCourtHearingCourtHousesCourtroomsPath())
            .buildAndExpand(courtId, courtRoomId)
            .toUriString();
    }

    protected String buildCourtHearingCourtHousesByIdUrl(String courtId) {
        return UriComponentsBuilder
            .fromUriString(getAmpUrl())
            .path(getApiCpRefDataCourtHearingCourtHousesPath())
            .buildAndExpand(courtId)
            .toUriString();
    }


    public ResponseEntity<CourtHouse> getCourtHouseById(String accessToken, String caseUrn, String courtId, String courtRoomId) {
        try {
            String courtHouseAmpUrl = (courtRoomId == null || courtRoomId.isEmpty())
                ? buildCourtHearingCourtHousesByIdUrl(courtId)
                : buildCourtHearingCourtHousesAndCourtRoomsByIdUrl(courtId, courtRoomId);

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
