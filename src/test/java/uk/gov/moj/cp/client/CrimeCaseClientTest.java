package uk.gov.moj.cp.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class CrimeCaseClientTest {

    @Autowired
    private CrimeCaseClient crimeCaseClient;

    @MockitoBean
    private RestTemplate restTemplate;

    void shouldBuildCrimeCaseUrl() {
        Long id = 100L;
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-cases/0.0.2/cases/100/results";

        String actualUrl = crimeCaseClient.buildCrimeCaseUrl(id);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    void shouldReturnCaseDetails_whenRequestSucceeds() {
        Long id = 100L;
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-cases/0.0.2/cases/100/results";

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
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-cases/0.0.2/cases/100/results";

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
