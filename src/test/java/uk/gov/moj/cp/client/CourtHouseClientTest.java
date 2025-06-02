package uk.gov.moj.cp.client;

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
    void shouldGetCourthearingCourthousesById() {
        String id = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.4.0/courthouses/123";

        assertThat(courtHouseClient.getCourthearingCourthousesById(id)).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCourtHouseDetails_whenRequestIsSuccessful() {
        String id = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.4.0/courthouses/123";

        ResponseEntity<String> response = new ResponseEntity<>("Some mock response", HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity()),
            eq(String.class)
        )).thenReturn(response);

        HttpEntity<String> actual = courtHouseClient.getCourtHouseById(id);

        assertThat(actual).isNotNull();
        assertThat("Some mock response").isEqualTo(actual.getBody());
    }

    @Test
    void shouldLogErrorAndReturnNull_whenRestTemplateThrowsException() {
        String id = "123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-refdata-courthearing-courthouses/0.4.0/courthouses/123";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtHouseClient.getRequestEntity()),
            eq(String.class)
        )).thenThrow(new RestClientException("Timeout"));

        HttpEntity<String> result = courtHouseClient.getCourtHouseById("INVALID_ID");

        assertThat(result).isNull();
    }
}

