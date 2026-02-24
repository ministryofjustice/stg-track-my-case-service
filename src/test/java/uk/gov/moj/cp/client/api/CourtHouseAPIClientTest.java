package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.Address;
import com.moj.generated.hmcts.CourtHouse;
import com.moj.generated.hmcts.CourtRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourtHouseAPIClientTest {

    private CourtHouseAPIClient courtHouseClient;

    private RestTemplate restTemplate;

    private final String ampUrl = "https://some.dev.environment.com";
    private final String ampSubscriptionKey = "some-amp-subscription-key";
    private final String apiCpRefDataCourtHearingCourtHousesCourtroomsPath = "/courthouses/{court_id}/courtrooms/{court_room_id}";
    private final String accessToken = "testToken";

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        courtHouseClient = new CourtHouseAPIClient(restTemplate) {
            @Override
            public String getAmpUrl() {
                return ampUrl;
            }

            @Override
            public String getAmpSubscriptionKey() {
                return ampSubscriptionKey;
            }

            @Override
            public String getApiCpRefDataCourtHearingCourtHousesCourtroomsPath() {
                return apiCpRefDataCourtHearingCourtHousesCourtroomsPath;
            }
        };
    }

    @Test
    void shouldBuildCourthearingCourthousesAndCourtRoomsByIdUrl() {
        String id = "123";
        String courtRoomId = "123";
        String expectedUrl = "https://some.dev.environment.com/courthouses/123/courtrooms/123";

        assertThat(courtHouseClient.buildCourtHearingCourtHousesAndCourtRoomsByIdUrl(id, courtRoomId)).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCourtHouseDetails_whenRequestIsSuccessful() {
        String courtId = "courtId-123";
        String courtRoomId = "courtRoomId-456";
        String expectedUrl = "https://some.dev.environment.com/courthouses/courtId-123/courtrooms/courtRoomId-456";

        Address address = new Address("1 High Street", null, null, null, "AA1 2BB", "London");
        CourtHouse courtHouse = new CourtHouse(CourtHouse.CourtHouseType.CROWN,
                                               "some-courtHouseCode",
                                               "some-courtHouseName",
                                               address,
                                               List.of(new CourtRoom(1, "some-courtRoomName-1")));
        ResponseEntity<CourtHouse> response = new ResponseEntity<>(
            courtHouse,
            HttpStatus.OK);
        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity(accessToken)),
            eq(CourtHouse.class)
        )).thenReturn(response);
        ResponseEntity<CourtHouse> actual = courtHouseClient.getCourtHouseById(accessToken, courtId, courtRoomId);

        assertThat(actual).isNotNull();
        assertThat(courtHouse).isEqualTo(actual.getBody());
    }

    @Test
    void shouldLogErrorAndReturnNull_whenRestTemplateThrowsException() {
        String courtId = "courtId-123";
        String courtRoomId = "courtRoomId-456";
        String expectedUrl = "https://some.dev.environment.com/courthouses/courtId-123/courtrooms/courtRoomId-456";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity(accessToken)),
            eq(CourtHouse.class)
        )).thenThrow(new RestClientException("Timeout"));

        HttpEntity<CourtHouse> result = courtHouseClient.getCourtHouseById(accessToken, courtId, courtRoomId);
        assertThat(result).isNull();
    }
}

