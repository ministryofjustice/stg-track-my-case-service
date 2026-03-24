package uk.gov.moj.cp.client.api;

import com.moj.generated.hmcts.ProsecutionCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProsecutionCaseAPIClientTest {

    private ProsecutionCaseAPIClient pcdAPIClient;

    private RestTemplate restTemplate;

    private final String ampUrl = "https://some.dev.environment.com";
    private final String ampSubscriptionKey = "some-amp-subscription-key";
    private final String apiCpPcdCourtstatusPath = "/cases/{case_urn}";
    private final String accessToken = "testToken";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        pcdAPIClient = new ProsecutionCaseAPIClient(restTemplate) {
            @Override
            public String getAmpUrl() {
                return ampUrl;
            }

            @Override
            public String getPcdSubscriptionKey() {
                return ampSubscriptionKey;
            }

            @Override
            public String getApiCpApiCpPcdCourtstatusPath() {
                return apiCpPcdCourtstatusPath;
            }
        };
    }

    @Test
    void shouldBuildCourtScheduleUrl() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://some.dev.environment.com/cases/CASE123";

        assertThat(pcdAPIClient.buildCourtScheduleUrl(caseUrn)).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCaseStatus_whenRequestIsSuccessful() {
        String caseUrn = "URN-456";
        String expectedUrl = "https://some.dev.environment.com/cases/URN-456";

        ProsecutionCase caseStatus = new ProsecutionCase("Active", "Reporting restrictions apply");
        ResponseEntity<ProsecutionCase> response = new ResponseEntity<>(caseStatus, HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(pcdAPIClient.getRequestEntity(accessToken)),
            eq(ProsecutionCase.class)
        )).thenReturn(response);

        ResponseEntity<ProsecutionCase> actual = pcdAPIClient.getCaseStatus(accessToken, caseUrn);

        assertThat(actual).isNotNull();
        assertThat(caseStatus).isEqualTo(actual.getBody());
    }

    @Test
    void shouldRethrowHttpStatusCodeException_whenRestTemplateReturnsHttpError() {
        String caseUrn = "URN-789";
        String expectedUrl = "https://some.dev.environment.com/cases/URN-789";

        HttpClientErrorException exception = HttpClientErrorException.create(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            HttpHeaders.EMPTY,
            null,
            null
        );

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(pcdAPIClient.getRequestEntity(accessToken)),
            eq(ProsecutionCase.class)
        )).thenThrow(exception);

        assertThatThrownBy(() -> pcdAPIClient.getCaseStatus(accessToken, caseUrn))
            .isInstanceOf(HttpClientErrorException.class)
            .hasMessageContaining("503");
    }
}
