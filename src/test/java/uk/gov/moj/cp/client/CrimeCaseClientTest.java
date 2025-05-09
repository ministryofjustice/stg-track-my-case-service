package uk.gov.moj.cp.client;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrimeCaseClientTest {

    @InjectMocks
    private CrimeCaseClient crimeCaseClient;

    @Mock
    private RestTemplate restTemplate;

    @Test
    void testGetCaseById_ValidResponse() {
        Long caseId = 1L;
        String mockResponse = "[{\"resultText\":\"Guilty plea accepted by the court.\"}]";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = ResponseEntity.ok(mockResponse);

        when(restTemplate.exchange(
            eq(String.format("https://virtserver.swaggerhub.com/HMCTS-DTS/api-cp-crime-cases/0.0.1/cases/%s/results", caseId)),
            eq(HttpMethod.GET),
            eq(entity),
            eq(String.class)
        )).thenReturn(responseEntity);

        HttpEntity<String> response = crimeCaseClient.getCaseById(caseId);

        assertNotNull(response);
        assertEquals(mockResponse, response.getBody());
    }


}
