package uk.gov.moj.cp.client;

import com.moj.generated.hmcts.CourtHouse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class CourtHouseClientTest {

    @Autowired
    private CourtHouseClient courtHouseClient;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void shouldBuildCourthearingCourthousesByIdUrl() {
        String id = "123";
        String courtRoomId = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.7.3/courthouses/123/courtrooms/123";

        assertThat(courtHouseClient.buildCourthearingCourthousesByIdUrl(id, courtRoomId)).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCourtHouseDetails_whenRequestIsSuccessful() {
        String id = "123";
        String courtRoomId = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.7.3/courthouses/123/courtrooms/123";

        ResponseEntity<CourtHouse> response = new ResponseEntity<>(new CourtHouse(
            CourtHouse.CourtHouseType.CROWN, "code", null, null, null),
                                                                   HttpStatus.OK);
        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity()),
            eq(CourtHouse.class)
        )).thenReturn(response);
        HttpEntity<CourtHouse> actual = courtHouseClient.getCourtHouseById(id, courtRoomId);

        assertThat(actual).isNotNull();
        assertThat("code").isEqualTo(actual.getBody().getCourtHouseCode());
        assertThat(CourtHouse.CourtHouseType.CROWN.value()).isEqualTo(actual.getBody().getCourtHouseType().toString());
    }

    @Test
    void shouldLogErrorAndReturnNull_whenRestTemplateThrowsException() {
        String id = "123";
        String courtRoomId = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.7.3/courthouses/123/courtrooms/123";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity()),
            eq(CourtHouse.class)
        )).thenThrow(new RestClientException("Timeout"));

        HttpEntity<CourtHouse> result = courtHouseClient.getCourtHouseById(id, courtRoomId);
        assertThat(result).isNull();
    }
}

