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

class JudgesClientTest {

    @Autowired
    private JudgesClient judgesClient;

    @MockitoBean
    private RestTemplate restTemplate;

    private final String ampUrl = "https://some.dev.environment.com";
    private final String ampSubscriptionKey = "some-amp-subscription-key";
    private final String apiRefDataCourtHearingJudgesPath = "https://test-url/0.3.10";

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        judgesClient = new JudgesClient(restTemplate) {
            @Override
            public String getAmpUrl() {
                return ampUrl;
            }

            @Override
            public String getAmpSubscriptionKey() {
                return ampSubscriptionKey;
            }

            @Override
            public String getApiRefDataCourtHearingJudgesPath() {
                return apiRefDataCourtHearingJudgesPath;
            }
        };
    }

    @Test
    void shouldBuildJudgesUrlCorrectly() {
        Long id = 456L;
        String expectedUrl = "https://test-url/0.3.10/judges/456";

        String actualUrl = judgesClient.buildJudgesUrl(id);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnJudgeDetails_whenRequestSucceeds() {
        Long id = 456L;
        String expectedUrl = "https://test-url/0.3.10/judges/456";

        ResponseEntity<String> mockResponse = new ResponseEntity<>("Mock judge info", HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(judgesClient.getRequestEntity()),
            eq(String.class)
        )).thenReturn(mockResponse);

        ResponseEntity<String> response = judgesClient.getJudgesById(id);

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isEqualTo("Mock judge info");
    }

    @Test
    void shouldReturnNull_whenRestTemplateThrowsException() {
        Long id = 456L;
        String expectedUrl = "https://test-url/0.3.10/judges/456";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(judgesClient.getRequestEntity()),
            eq(String.class)
        )).thenThrow(new RestClientException("Connection error"));

        ResponseEntity<String> response = judgesClient.getJudgesById(id);
        assertThat(response).isNull();
    }
}
