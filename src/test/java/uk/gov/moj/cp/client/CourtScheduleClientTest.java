package uk.gov.moj.cp.client;

import com.moj.generated.hmcts.CourtSchedule;
import com.moj.generated.hmcts.CourtScheduleSchema;
import com.moj.generated.hmcts.CourtSitting;
import com.moj.generated.hmcts.Hearing;
import com.moj.generated.hmcts.WeekCommencing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.moj.cp.service.MockCourtScheduleClient;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourtScheduleClientTest {

    private CourtScheduleClient courtScheduleClient;
    private MockCourtScheduleClient mockCourtScheduleClient;

    private RestTemplate restTemplate;

    private final String ampUrl = "https://some.dev.environment.com";
    private final String ampSubscriptionKey = "some-amp-subscription-key";
    private final String apiCpCrimeSchedulingandlistingCourtschedulePath = "/case/{case_urn}/courtschedule";
    private final String accessToken = "testToken";

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        mockCourtScheduleClient = new MockCourtScheduleClient(){
            @Override
            public Boolean getUseMockData() {
                return false;
            }
        };
        courtScheduleClient = new CourtScheduleClient(restTemplate, mockCourtScheduleClient) {
            @Override
            public String getAmpUrl() {
                return ampUrl;
            }

            @Override
            public String getAmpSubscriptionKey() {
                return ampSubscriptionKey;
            }

            @Override
            public String getApiCpCrimeSchedulingandlistingCourtschedulePath() {
                return apiCpCrimeSchedulingandlistingCourtschedulePath;
            }
        };
    }

    @Test
    void shouldBuildCourtScheduleUrl() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://some.dev.environment.com/case/CASE123/courtschedule";

        String actualUrl = courtScheduleClient.buildCourtScheduleUrl(caseUrn);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCourtSchedule_whenRequestSucceeds() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://some.dev.environment.com/case/CASE123/courtschedule";

        List<CourtSitting> courtSittings = List.of(
            new CourtSitting(
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                "some-judiciaryId", "some-courtHouse",
                "some-courtRoom"
            )
        );
        WeekCommencing wc = new WeekCommencing(
            null,
            LocalDate.now(),
            LocalDate.now().plusDays(7),
            2
        );

        Hearing hearing = new Hearing(
            "1", "some-hearingType",
            "some-hearingDescription", "some-listNote",
            wc,
            courtSittings
        );
        CourtScheduleSchema courtScheduleSchema = new CourtScheduleSchema(List.of(new CourtSchedule(List.of(hearing))));
        ResponseEntity<CourtScheduleSchema> response = new ResponseEntity<>(
            courtScheduleSchema,
            HttpStatus.OK
        );

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtScheduleClient.getRequestEntity(accessToken)),
            eq(CourtScheduleSchema.class)
        )).thenReturn(response);

        ResponseEntity<CourtScheduleSchema> actual = courtScheduleClient.getCourtScheduleByCaseUrn(accessToken,
                                                                                                   caseUrn);

        assertThat(actual).isNotNull();
        assertThat(courtScheduleSchema).isEqualTo(actual.getBody());
    }

    @Test
    void shouldReturnNull_whenRestTemplateThrowsException() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://some.dev.environment.com/case/CASE123/courtschedule";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtScheduleClient.getRequestEntity(accessToken)),
            eq(CourtScheduleSchema.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        ResponseEntity<CourtScheduleSchema> response = courtScheduleClient.getCourtScheduleByCaseUrn(accessToken,
                                                                                                     caseUrn);
        assertThat(response).isNull();
    }
}
