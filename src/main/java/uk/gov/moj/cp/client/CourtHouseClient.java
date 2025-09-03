package uk.gov.moj.cp.client;

import com.moj.generated.hmcts.CourtHouse;
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
    private String courthearingCourthousesVersion;

    private static final String COURTHOUSES_BY_ID_AND_ROOM_ID = "courthouses/{id}/courtrooms/{court_room_id}";

    protected String buildCourthearingCourthousesByIdUrl(String id, String courtRoomId) {
        return UriComponentsBuilder
            .fromUri(URI.create(courthearingCourthousesUrl))
            .pathSegment(courthearingCourthousesVersion)
            .pathSegment(COURTHOUSES_BY_ID_AND_ROOM_ID)
            .buildAndExpand(id, courtRoomId)
            .toUriString();
    }

    public ResponseEntity<CourtHouse> getCourtHouseById(String id, String courtRoomId) {
        try {
            ResponseEntity<CourtHouse> responseEntity = restTemplate.exchange(
                buildCourthearingCourthousesByIdUrl(id, courtRoomId),
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
        return new HttpEntity<>(headers);
    }

}
