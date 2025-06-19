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
class CourtScheduleClientTest {

    @Autowired
    private CourtScheduleClient courtScheduleClient;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void shouldBuildCourtScheduleUrl() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-schedulingandlisting-courtschedule/0.4.1/case/CASE123/courtschedule";

        String actualUrl = courtScheduleClient.buildCourtScheduleUrl(caseUrn);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void shouldReturnCourtSchedule_whenRequestSucceeds() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-schedulingandlisting-courtschedule/0.4.1/case/CASE123/courtschedule";

        ResponseEntity<String> mockResponse = new ResponseEntity<>("Mock court schedule", HttpStatus.OK);

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtScheduleClient.getRequestEntity()),
            eq(String.class)
        )).thenReturn(mockResponse);

        ResponseEntity<String> actualResponse = courtScheduleClient.getCourtScheduleByCaseUrn(caseUrn);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getBody()).isEqualTo("Mock court schedule");
    }

    @Test
    void shouldReturnNull_whenRestTemplateThrowsException() {
        String caseUrn = "CASE123";
        String expectedUrl = "https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-schedulingandlisting-courtschedule/0.4.1/case/CASE123/courtschedule";

        when(restTemplate.exchange(
            eq(expectedUrl),
            eq(HttpMethod.GET),
            eq(courtScheduleClient.getRequestEntity()),
            eq(String.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        ResponseEntity<String> response = courtScheduleClient.getCourtScheduleByCaseUrn(caseUrn);

        assertThat(response).isNull();
    }
}
