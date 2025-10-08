package uk.gov.moj.cp.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrimeCaseClientTest {

    @Autowired
    private CrimeCaseClient crimeCaseClient;

    @MockitoBean
    private RestTemplate restTemplate;

    private final String ampUrl = "https://some.dev.environment.com";
    private final String ampSubscriptionKey = "some-amp-subscription-key";
    private final String apiCrimeCasesUrl = "https://test-url";
    public static final String CASES_CASE_ID_RESULTS = "/cases/{case_id}/results";
    public static final String VERSION = "0.7.1";

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        crimeCaseClient = new CrimeCaseClient(restTemplate) {
            @Override
            public String getAmpUrl() {
                return ampUrl;
            }

            @Override
            public String getAmpSubscriptionKey() {
                return ampSubscriptionKey;
            }

            @Override
            public String getCrimeCaseUrl() {
                return apiCrimeCasesUrl;
            }

            @Override
            public String getCrimeCaseVersion() {
                return VERSION;
            }

            @Override
            public String getCrimeCasePath() {
                return CASES_CASE_ID_RESULTS;
            }
        };
    }

    @Test
    void shouldBuildCrimeCaseUrl() {
        Long id = 100L;
        String expectedUrl = "https://test-url/0.7.1/cases/100/results";

        String actualUrl = crimeCaseClient.buildCrimeCaseUrl(id);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCaseDetails_whenRequestSucceeds() {
        Long id = 100L;
        String expectedUrl = "https://test-url/0.7.1/cases/100/results";

        ResponseEntity<String> mockResponse = new ResponseEntity<>("Mock case data", HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(crimeCaseClient.getRequestEntity()),
            eq(String.class)
        )).thenReturn(mockResponse);

        ResponseEntity<String> actualResponse = crimeCaseClient.getCaseById(id);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getBody()).isEqualTo("Mock case data");
    }

    @Test
    void shouldReturnNull_whenRestTemplateThrowsException() {
        Long id = 100L;
        String expectedUrl = "https://test-url/0.7.1/cases/100/results";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(crimeCaseClient.getRequestEntity()),
            eq(String.class)
        )).thenThrow(new RestClientException("Timeout"));

        ResponseEntity<String> response = crimeCaseClient.getCaseById(id);

        assertThat(response).isNull();
    }
}
